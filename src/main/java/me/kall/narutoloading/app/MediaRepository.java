package me.kall.narutoloading.app;

import me.kall.narutoloading.app.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.app.ffmpeg.data.VideoArgs;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("UnusedReturnValue")
public final class MediaRepository {
    private static final List<String> VIDEO_EXTENSIONS = List.of("mp4", "mkv", "webm", "mov", "m4v", "avi", "flv");
    private static final List<String> AUDIO_EXTENSIONS = List.of("ogg", "opus", "mp3", "flac", "wav", "m4a", "aac");

    public static final String AUDIO_SUFFIX = ".audio";

    private static final long IDLE_RESCAN_NANOS = 3_000_000_000L;

    public enum Mode {
        RANDOM, SEQUENTIAL, FIXED;

        public static @NotNull Mode parse(@Nullable String value) {
            if (value == null) return RANDOM;
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return RANDOM;
            }
        }
    }

    public record Media(@NotNull String name, @Nullable VideoArgs video, @Nullable AudioArgs audio) {}

    private record Group(String name, @Nullable Path video, @Nullable Path audio) {}

    private record Probed<T>(long modified, long size, @Nullable T value) {}

    public static boolean isMediaFile(@NotNull String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
        return VIDEO_EXTENSIONS.contains(extension) || AUDIO_EXTENSIONS.contains(extension);
    }

    public static @NotNull String mediaName(@NotNull String fileName) {
        String baseName = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
        return AUDIO_EXTENSIONS.contains(extension) ? stripAudioSuffix(baseName) : baseName;
    }

    public static boolean wouldOverwrite(@NotNull String name, @NotNull String fileName) {
        String baseName = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);

        if (VIDEO_EXTENSIONS.contains(extension)) return baseName.equalsIgnoreCase(name.trim());
        if (AUDIO_EXTENSIONS.contains(extension)) return baseName.equalsIgnoreCase(name.trim() + AUDIO_SUFFIX);

        return false;
    }

    private static @NotNull String stripAudioSuffix(@NotNull String baseName) {
        int at = baseName.length() - AUDIO_SUFFIX.length();
        if (at <= 0) return baseName;
        if (!baseName.regionMatches(true, at, AUDIO_SUFFIX, 0, AUDIO_SUFFIX.length())) return baseName;
        return baseName.substring(0, at);
    }

    private final Supplier<Path> sourcesDir;
    private final Supplier<Mode> mode;
    private final Supplier<String> fixedName;
    private final Function<String, CompletableFuture<@Nullable VideoArgs>> videoReader;
    private final Function<String, CompletableFuture<@Nullable AudioArgs>> audioReader;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NarutoMedia");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicBoolean scanning = new AtomicBoolean();
    private volatile long lastScanStart = 0L;
    private volatile boolean everScanned = false;

    private volatile List<Media> ready = List.of();

    private final Map<Path, Probed<VideoArgs>> videoCache = new ConcurrentHashMap<>();
    private final Map<Path, Probed<AudioArgs>> audioCache = new ConcurrentHashMap<>();

    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    private final Object lock = new Object();

    private @Nullable Media current;
    private @Nullable String lastName;

    public MediaRepository(@NotNull Supplier<Path> sourcesDir, @NotNull Supplier<Mode> mode, @NotNull Supplier<String> fixedName, @NotNull Function<String, CompletableFuture<@Nullable VideoArgs>> videoReader, @NotNull Function<String, CompletableFuture<@Nullable AudioArgs>> audioReader) {
        this.sourcesDir = sourcesDir;
        this.mode = mode;
        this.fixedName = fixedName;
        this.videoReader = videoReader;
        this.audioReader = audioReader;
    }

    public @Nullable Media current() {
        synchronized (this.lock) {
            if (this.current == null) this.select();
            if (this.current == null) this.refresh(false, null);
            return this.current;
        }
    }

    public boolean hasSource() {
        return this.current() != null;
    }

    public @NotNull List<Media> all() {
        return this.ready;
    }

    public boolean select(@NotNull String name) {
        synchronized (this.lock) {
            for (Media media : this.ready) {
                if (media.name().equalsIgnoreCase(name)) {
                    this.current = media;
                    this.lastName = media.name();
                    return true;
                }
            }
        }

        return false;
    }

    public @Nullable VideoArgs peekVideo() {
        Media media = this.current();
        return media == null ? null : media.video();
    }

    public @Nullable AudioArgs peekAudio() {
        Media media = this.current();
        return media == null ? null : media.audio();
    }

    public @Nullable Media advance() {
        Media picked;
        synchronized (this.lock) {
            this.select();
            picked = this.current;
        }

        this.refresh();
        return picked;
    }

    public void refresh() {
        this.refresh(true, null);
    }

    public void refresh(@NotNull Runnable onDone) {
        this.refresh(true, onDone);
    }

    public void invalidate() {
        this.videoCache.clear();
        this.audioCache.clear();
        this.submit(() -> this.refresh(true, null));
    }

    private void select() {
        List<Media> candidates = new ArrayList<>();
        for (Media media : this.ready) {
            if (exists(media)) candidates.add(media);
        }

        Media picked = candidates.isEmpty() ? null : this.pick(candidates);
        this.current = picked;
        if (picked != null) this.lastName = picked.name();
    }

    private @NotNull Media pick(@NotNull List<Media> candidates) {
        if (candidates.size() == 1) return candidates.get(0);

        Mode mode = this.mode.get();

        if (mode == Mode.FIXED) {
            String wanted = this.fixedName.get();
            if (wanted != null && !wanted.isBlank()) {
                for (Media media : candidates) {
                    if (media.name().equalsIgnoreCase(wanted.trim())) return media;
                }

                if (this.warned.add("fixed:" + wanted)) {
                    System.err.println("[NarutoLoading] fixedName '" + wanted + "' was not found in sources, falling back to random selection.");
                }
            }
            mode = Mode.RANDOM;
        }

        if (mode == Mode.SEQUENTIAL) {
            int next = 0;
            for (int i = 0; i < candidates.size(); i++) {
                if (candidates.get(i).name().equalsIgnoreCase(this.lastName)) {
                    next = (i + 1) % candidates.size();
                    break;
                }
            }
            return candidates.get(next);
        }

        List<Media> pool = new ArrayList<>(candidates.size());
        for (Media media : candidates) {
            if (!media.name().equalsIgnoreCase(this.lastName)) pool.add(media);
        }

        if (pool.isEmpty()) pool = candidates;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private static boolean exists(@NotNull Media media) {
        VideoArgs video = media.video();
        AudioArgs audio = media.audio();
        if (video != null && !Files.isRegularFile(Path.of(video.absPath()))) return false;
        return audio == null || Files.isRegularFile(Path.of(audio.absPath()));
    }

    private void refresh(boolean force, @Nullable Runnable onDone) {
        long now = System.nanoTime();
        if (!force && this.everScanned && now - this.lastScanStart < IDLE_RESCAN_NANOS) return;
        if (!this.scanning.compareAndSet(false, true)) {
            if (onDone != null) this.submit(() -> this.refresh(true, onDone));
            return;
        }

        this.everScanned = true;
        this.lastScanStart = now;

        try {
            this.executor.execute(() -> {
                try {
                    this.rebuild();
                } catch (Throwable throwable) {
                    System.err.println("[NarutoLoading] Exception scanning media sources: " + throwable.getMessage());
                    throwable.printStackTrace(System.err);
                } finally {
                    this.scanning.set(false);
                }

                if (onDone != null) onDone.run();
            });
        } catch (RuntimeException exception) {
            this.scanning.set(false);
        }
    }

    private void submit(@NotNull Runnable task) {
        try {
            this.executor.execute(task);
        } catch (RuntimeException exception) {
            System.err.println("[NarutoLoading] Exception scheduling a media rescan: " + exception.getMessage());
        }
    }

    private void rebuild() {
        List<Group> groups = this.scan();

        List<Media> result = new ArrayList<>(groups.size());
        Set<Path> seen = new HashSet<>();

        for (Group group : groups) {
            VideoArgs video = null;
            if (group.video() != null) {
                seen.add(group.video());
                video = probe(this.videoCache, group.video(), this.videoReader);
            }

            Path audioSource = group.audio() != null ? group.audio() : group.video();
            AudioArgs audio = null;
            if (audioSource != null) {
                seen.add(audioSource);
                audio = probe(this.audioCache, audioSource, this.audioReader);
            }

            if (video == null && audio == null) {
                if (this.warned.add("unusable:" + group.name())) System.err.println("[NarutoLoading] Skipping unusable media source '" + group.name() + "'.");
                continue;
            }

            result.add(new Media(group.name(), video, audio));
        }

        this.videoCache.keySet().retainAll(seen);
        this.audioCache.keySet().retainAll(seen);

        this.ready = List.copyOf(result);
    }

    private @NotNull List<Group> scan() {
        Path dir = this.resolveDir();
        if (!Files.isDirectory(dir)) return List.of();

        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException | UncheckedIOException exception) {
            System.err.println("[NarutoLoading] Exception listing sources directory " + dir + ": " + exception.getMessage());
            return List.of();
        }

        Map<String, Path[]> slots = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Path file : files) {
            String fileName = file.getFileName().toString();
            String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);

            boolean isVideo = VIDEO_EXTENSIONS.contains(extension);
            boolean isAudio = AUDIO_EXTENSIONS.contains(extension);
            if (!isVideo && !isAudio) continue;

            Path[] slot = slots.computeIfAbsent(mediaName(fileName), key -> new Path[2]);
            if (isVideo) slot[0] = better(slot[0], file, VIDEO_EXTENSIONS);
            if (isAudio) slot[1] = better(slot[1], file, AUDIO_EXTENSIONS);
        }

        List<Group> groups = new ArrayList<>(slots.size());
        for (Map.Entry<String, Path[]> entry : slots.entrySet()) {
            groups.add(new Group(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }

        return groups;
    }

    private static @NotNull Path better(@Nullable Path existing, @NotNull Path candidate, @NotNull List<String> priority) {
        if (existing == null) return candidate;
        return rank(candidate, priority) < rank(existing, priority) ? candidate : existing;
    }

    private static int rank(@NotNull Path path, @NotNull List<String> priority) {
        int index = priority.indexOf(FilenameUtils.getExtension(path.getFileName().toString()).toLowerCase(Locale.ROOT));
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private @NotNull Path resolveDir() {
        Path dir = this.sourcesDir.get();
        if (dir.isAbsolute()) return dir;
        return Path.of(System.getProperty("user.dir", ".")).resolve(dir);
    }

    private static <T> @Nullable T probe(@NotNull Map<Path, Probed<T>> cache, @NotNull Path path, @NotNull Function<String, CompletableFuture<@Nullable T>> reader) {
        long modified;
        long size;
        try {
            modified = Files.getLastModifiedTime(path).toMillis();
            size = Files.size(path);
        } catch (IOException exception) {
            cache.remove(path);
            return null;
        }

        Probed<T> cached = cache.get(path);
        if (cached != null && cached.modified() == modified && cached.size() == size) return cached.value();

        T value;
        try {
            value = reader.apply(path.toAbsolutePath().toString()).join();
        } catch (CompletionException | CancellationException exception) {
            value = null;
        }

        if (value == null) {
            cache.remove(path);
            return null;
        }

        cache.put(path, new Probed<>(modified, size, value));
        return value;
    }
}