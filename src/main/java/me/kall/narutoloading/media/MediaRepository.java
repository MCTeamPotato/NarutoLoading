package me.kall.narutoloading.media;

import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.core.GamePaths;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("UnusedReturnValue")
public final class MediaRepository {
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

    private record Probed<T>(long modified, long size, @Nullable T value) {}

    private final Supplier<Path> sourcesDir;
    private final Supplier<Mode> mode;
    private final Supplier<String> fixedName;
    private final Function<String, CompletableFuture<@Nullable VideoArgs>> videoReader;
    private final Function<String, CompletableFuture<@Nullable AudioArgs>> audioReader;
    private final ExecutorService importExecutor;

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
    private @Nullable String pendingPath;

    public MediaRepository(@NotNull Supplier<Path> sourcesDir, @NotNull Supplier<Mode> mode, @NotNull Supplier<String> fixedName, @NotNull Function<String, CompletableFuture<@Nullable VideoArgs>> videoReader, @NotNull Function<String, CompletableFuture<@Nullable AudioArgs>> audioReader, @NotNull ExecutorService importExecutor) {
        this.sourcesDir = sourcesDir;
        this.mode = mode;
        this.fixedName = fixedName;
        this.videoReader = videoReader;
        this.audioReader = audioReader;
        this.importExecutor = importExecutor;
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

    public void adopt(@Nullable String absPath) {
        if (absPath == null || absPath.isBlank()) return;

        synchronized (this.lock) {
            for (Media media : this.ready) {
                if (media.video() != null && media.video().absPath().equals(absPath)) {
                    this.current = media;
                    this.lastName = media.name();
                    return;
                }
            }

            this.pendingPath = absPath;
        }
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

    @Contract("_ -> new")
    public @NotNull CompletableFuture<@Nullable Path> importMedia(@NotNull Path source) {
        return CompletableFuture.supplyAsync(() -> this.copyMedia(source), this.importExecutor);
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

        Media picked = MediaSelector.pick(candidates, Media::name, this.mode.get(), this.fixedName.get(), this.lastName, true);

        this.current = picked;
        if (picked != null) this.lastName = picked.name();
    }

    private static boolean exists(@NotNull Media media) {
        VideoArgs video = media.video();
        AudioArgs audio = media.audio();
        if (video != null && !Files.isRegularFile(Path.of(video.absPath()))) return false;
        return audio == null || Files.isRegularFile(Path.of(audio.absPath()));
    }

    private @Nullable Path copyMedia(@NotNull Path source) {
        Path directory = GamePaths.resolve(this.sourcesDir.get());
        Path target = uniqueTarget(directory, source.getFileName().toString());

        try {
            Files.createDirectories(directory);
            if (!source.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) Files.copy(source, target);
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Exception copying " + source + " to " + target + ": " + exception.getMessage());
            return null;
        }

        return target;
    }

    private static @NotNull Path uniqueTarget(@NotNull Path directory, @NotNull String fileName) {
        Path target = directory.resolve(fileName);
        if (!Files.exists(target)) return target;

        String baseName = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName);
        String suffix = extension.isEmpty() ? "" : "." + extension;

        for (int index = 2; ; index++) {
            Path candidate = directory.resolve(baseName + " (" + index + ")" + suffix);
            if (!Files.exists(candidate)) return candidate;
        }
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
        List<MediaScanner.Group> groups = MediaScanner.scan(GamePaths.resolve(this.sourcesDir.get()));

        List<Media> result = new ArrayList<>(groups.size());
        Set<Path> seen = new HashSet<>();

        for (MediaScanner.Group group : groups) {
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

        String pending = this.pendingPath;

        if (pending != null) {
            synchronized (this.lock) {
                for (Media media : this.ready) {
                    if (media.video() != null && media.video().absPath().equals(pending)) {
                        this.current = media;
                        this.lastName = media.name();
                        break;
                    }
                }

                this.pendingPath = null;
            }
        }
    }

    public static @Nullable MediaSource pickSource(@NotNull Path directory, @NotNull Mode mode, @Nullable String fixedName) {
        List<MediaScanner.Group> groups = MediaScanner.scan(directory);

        List<MediaScanner.Group> candidates = new ArrayList<>(groups.size());
        for (MediaScanner.Group group : groups) {
            if (group.video() != null) candidates.add(group);
        }

        MediaScanner.Group picked = MediaSelector.pick(candidates, MediaScanner.Group::name, mode, fixedName, null, false);
        if (picked == null) return null;

        Path video = picked.video();
        Path audio = picked.audio();

        if (video == null) return null;

        return new MediaSource(video.toAbsolutePath().toString(), audio == null ? null : audio.toAbsolutePath().toString());
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
