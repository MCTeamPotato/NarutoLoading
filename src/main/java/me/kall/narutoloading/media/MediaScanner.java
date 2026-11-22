package me.kall.narutoloading.media;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class MediaScanner {
    public record Group(@NotNull String name, @Nullable Path video, @Nullable Path audio) {}

    private MediaScanner() {}

    public static @NotNull List<Group> scan(@NotNull Path dir) {
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

            boolean isVideo = MediaNaming.VIDEO_EXTENSIONS.contains(extension);
            boolean isAudio = MediaNaming.AUDIO_EXTENSIONS.contains(extension);
            if (!isVideo && !isAudio) continue;

            Path[] slot = slots.computeIfAbsent(MediaNaming.mediaName(fileName), key -> new Path[2]);
            if (isVideo) slot[0] = better(slot[0], file, MediaNaming.VIDEO_EXTENSIONS);
            if (isAudio) slot[1] = better(slot[1], file, MediaNaming.AUDIO_EXTENSIONS);
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
}
