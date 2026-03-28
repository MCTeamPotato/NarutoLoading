package me.kall.narutoloading.app.ytdlp;

import me.kall.narutoloading.app.Executable;
import me.kall.narutoloading.data.Paths;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class YtDlp {
    @Contract("_, _ -> new")
    private static String @NotNull [] audioCommand(String url, String outputTemplate) {
        return new String[]{Paths.YT_DLP.toString(), url, "-o", outputTemplate, "-x", "--audio-format", "mp3", "--audio-quality", "0", "--no-playlist", "--progress", "--ffmpeg-location", Paths.FFMPEG.toString()};
    }

    @Contract("_, _ -> new")
    private static String @NotNull [] videoCommand(String url, String outputTemplate) {
        return new String[]{Paths.YT_DLP.toString(), url, "-o", outputTemplate, "--format", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best", "--merge-output-format", "mp4", "--no-playlist", "--progress", "--ffmpeg-location", Paths.FFMPEG.toString()};
    }

    public static @Nullable Path downloadVideo(String url, @NotNull Path outputDirectory, String outputName) {
        Executable.executeCommand(videoCommand(url, outputDirectory.resolve(outputName + ".%(ext)s").toString()), true);
        return getDownloaded(outputDirectory, outputName);
    }

    public static @Nullable Path downloadAudio(String url, @NotNull Path outputDirectory, String outputName) {
        Executable.executeCommand(audioCommand(url, outputDirectory.resolve(outputName + ".%(ext)s").toString()), true);
        return getDownloaded(outputDirectory, outputName);
    }

    private static @Nullable Path getDownloaded(Path outputDirectory, String outputName) {
        try (Stream<Path> stream = Files.list(outputDirectory)) {
            return stream.filter(path -> path.getFileName().toString().startsWith(outputName + ".")).findFirst().map(Path::toAbsolutePath).orElse(null);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}