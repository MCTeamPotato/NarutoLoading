package me.kall.narutoloading.app.ytdlp;

import me.kall.narutoloading.app.Executable;
import me.kall.narutoloading.app.MediaRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class YtDlp {
    public final AtomicReference<@Nullable String> ytdlp = new AtomicReference<>();

    public @Nullable String downloadVideo(String url, Path outputDirectory, String name, @Nullable Consumer<String> progress) {
        return this.download(url, outputDirectory, name, "bestvideo", progress);
    }

    public @Nullable String downloadAudio(String url, Path outputDirectory, String name, @Nullable Consumer<String> progress) {
        return this.download(url, outputDirectory, name + MediaRepository.AUDIO_SUFFIX, "bestaudio", progress);
    }

    private @Nullable String download(String url, Path outputDirectory, String stem, String format, @Nullable Consumer<String> progress) {
        String ytdlp = this.ytdlp.get();
        if (ytdlp == null) return null;

        Path record;
        try {
            Files.createDirectories(outputDirectory);
            record = Files.createTempFile("narutoloading-ytdlp", ".txt");
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Exception preparing download. Url: " + url + ". Output Directory: " + outputDirectory + ". Exception: " + exception.getMessage());
            return null;
        }

        try {
            String output = Executable.runCommand(progress, ytdlp, url, "-o", outputDirectory.resolve(template(stem) + ".%(ext)s").toString(), "--format", format, "--no-playlist", "--progress", "--no-simulate", "--force-overwrites", "--print-to-file", "after_move:%(filepath)s", record.toString());

            if (output == null) return null;

            String recorded = lastNonEmptyLine(Files.readString(record));
            if (recorded == null) {
                System.err.println("[NarutoLoading] yt-dlp did not report an output filepath for url: " + url);
                return null;
            }

            return recorded;
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Exception reading download result for url: " + url + ". Exception: " + exception.getMessage());
            return null;
        } finally {
            deleteStale(record);
        }
    }

    private static @NotNull String template(@NotNull String stem) {
        return stem.replace("%", "%%");
    }

    private static @Nullable String lastNonEmptyLine(@NotNull String text) {
        String last = null;
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) last = trimmed;
        }

        return last;
    }

    private static void deleteStale(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Exception deleting stale " + path + ": " + exception.getMessage());
        }
    }
}
