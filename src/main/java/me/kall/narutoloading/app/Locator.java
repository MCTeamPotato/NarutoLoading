package me.kall.narutoloading.app;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class Locator {
    static final boolean WINDOWS = File.separatorChar == '\\';
    private static final int SEARCH_DEPTH = 4;

    private Locator() {}

    static final class Index {
        private final Map<Path, Map<String, String>> directories = new HashMap<>();

        private @Nullable String find(@NotNull Path directory, @NotNull String target) {
            Map<String, String> entries = this.directories.get(directory);
            if (entries == null) {
                entries = walk(directory);
                this.directories.put(directory, entries);
            }

            for (String name : fileNames(target)) {
                String located = entries.get(key(name));
                if (located != null) return located;
            }

            return null;
        }

        private static @NotNull Map<String, String> walk(@NotNull Path directory) {
            Map<String, String> entries = new HashMap<>();
            if (!Files.isDirectory(directory)) return entries;

            try (Stream<Path> stream = Files.walk(directory, SEARCH_DEPTH)) {
                stream.filter(Files::isRegularFile)
                        .forEach(path -> entries.putIfAbsent(key(path.getFileName().toString()), path.toAbsolutePath().toString()));
            } catch (IOException | UncheckedIOException exception) {
                System.err.println("[NarutoLoading] Exception searching " + directory + ": " + exception.getMessage());
            }

            return entries;
        }

        private static @NotNull String key(@NotNull String fileName) {
            return WINDOWS ? fileName.toLowerCase(Locale.ROOT) : fileName;
        }
    }

    public static @Nullable String locate(@NotNull String target, @NotNull Path gameDir, @Nullable String configuredDir) {
        return locate(target, gameDir, null, configuredDir == null || configuredDir.isBlank() ? null : Path.of(configuredDir), new Index());
    }

    static @Nullable String locate(@NotNull String target, @NotNull Path gameDir, @Nullable String neighbor, @Nullable Path configuredDir, @NotNull Index index) {
        String located = null;

        if (configuredDir != null && !configuredDir.toString().isEmpty()) {
            if (configuredDir.isAbsolute()) {
                located = index.find(configuredDir, target);
            } else {
                System.err.println("[NarutoLoading] Configured directory for " + target + " must be absolute: " + configuredDir);
            }
        }

        if (located == null) located = findInPath(target);

        if (located == null && neighbor != null) {
            File parent = new File(neighbor).getParentFile();
            located = parent == null ? null : index.find(parent.toPath(), target);
        }

        if (located == null) located = index.find(gameDir, target);

        if (located == null) {
            System.err.println("[NarutoLoading] Cannot find executable " + target
                    + (configuredDir == null || configuredDir.toString().isEmpty() ? "" : " in " + configuredDir)
                    + ", system PATH, or " + gameDir + " (depth " + SEARCH_DEPTH + ")");
        } else {
            System.out.println("[NarutoLoading] Located executable " + target + " at " + located);
        }

        return located;
    }

    private static @Nullable String findInPath(@NotNull String name) {
        String path = System.getenv("PATH");
        if (path == null) return null;

        for (String entry : path.split(File.pathSeparator)) {
            String dir = entry.trim();
            if (dir.isEmpty()) continue;
            if (dir.length() > 1 && dir.charAt(0) == '"' && dir.charAt(dir.length() - 1) == '"') {
                dir = dir.substring(1, dir.length() - 1);
            }

            String located = findIn(new File(dir), name);
            if (located != null) return located;
        }

        return null;
    }

    private static @Nullable String findIn(@NotNull File directory, @NotNull String name) {
        for (String fileName : fileNames(name)) {
            File candidate = new File(directory, fileName);
            if (candidate.isFile()) return candidate.getAbsolutePath();
        }

        return null;
    }

    private static @NotNull @Unmodifiable List<String> fileNames(@NotNull String name) {
        return WINDOWS ? List.of(name + ".exe", name) : List.of(name);
    }
}
