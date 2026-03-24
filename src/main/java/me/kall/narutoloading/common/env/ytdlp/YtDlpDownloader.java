package me.kall.narutoloading.common.env.ytdlp;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class YtDlpDownloader {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "YtDlpDownloader");
        thread.setDaemon(true);
        return thread;
    });

    public enum DownloadType {
        VIDEO,
        AUDIO
    }

    public static @Nullable CompletableFuture<Void> download(@NotNull String ytDlpPath, @NotNull String url, @NotNull Path outputDir, @NotNull String outputName, @NotNull DownloadType type, @Nullable Consumer<String> onProgress, @Nullable Consumer<DownloadResult> onComplete) {
        if (ytDlpPath.isBlank() || !new File(ytDlpPath).exists()) {
            if (onComplete != null) onComplete.accept(new DownloadResult(false, null, null, "yt-dlp not found"));
            return null;
        }

        return CompletableFuture.runAsync(() -> {
            DownloadResult result = new DownloadResult(false, null, null, null);

            try {
                outputDir.toFile().mkdirs();

                switch (type) {
                    case VIDEO -> result = downloadVideo(ytDlpPath, url, outputDir, outputName, onProgress);
                    case AUDIO -> result = downloadAudio(ytDlpPath, url, outputDir, outputName, onProgress);
                }

            } catch (Exception e) {
                result = new DownloadResult(false, null, null, e.getMessage());
            }

            if (onComplete != null) {
                onComplete.accept(result);
            }
        }, EXECUTOR);
    }

    @Contract("_, _, _, _, _ -> new")
    private static @NotNull DownloadResult downloadVideo(String ytDlpPath, String url, Path outputDir, String outputName, Consumer<String> onProgress) {
        try {
            String outputTemplate = outputDir.resolve(outputName + ".%(ext)s").toString();

            List<String> command = buildVideoCmd(ytDlpPath, url, outputTemplate);

            int exitCode = executeCommand(command, onProgress);

            if (exitCode == 0) {
                String videoPath = findFileInDirectory(outputDir, outputName, VIDEO_EXTENSIONS);
                if (videoPath != null) {
                    return new DownloadResult(true, videoPath, null, null);
                } else {
                    return new DownloadResult(false, null, null, "Video file not found after download");
                }
            } else {
                return new DownloadResult(false, null, null, "yt-dlp exited with code: " + exitCode);
            }

        } catch (Exception e) {
            return new DownloadResult(false, null, null, e.getMessage());
        }
    }

    private static @NotNull List<String> buildVideoCmd(String ytDlpPath, String url, String outputTemplate) {
        List<String> command = new ArrayList<>();
        command.add(ytDlpPath);
        command.add(url);
        command.add("-o");
        command.add(outputTemplate);
        command.add("--format");
        command.add("bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best");
        command.add("--merge-output-format");
        command.add("mp4");
        command.add("--no-playlist");
        command.add("--progress");
        command.add("--ffmpeg-location");
        command.add(BaseEnv.ffmpegProvider.absoluteFFmpeg);
        return command;
    }

    @Contract("_, _, _, _, _ -> new")
    private static @NotNull DownloadResult downloadAudio(String ytDlpPath, String url, Path outputDir, String outputName, Consumer<String> onProgress) {
        try {
            String outputTemplate = outputDir.resolve(outputName + ".%(ext)s").toString();

            List<String> command = buildAudioCmd(ytDlpPath, url, outputTemplate);

            int exitCode = executeCommand(command, onProgress);

            if (exitCode == 0) {
                String audioPath = findFileInDirectory(outputDir, outputName, AUDIO_EXTENSIONS);
                if (audioPath != null) {
                    return new DownloadResult(true, null, audioPath, null);
                } else {
                    return new DownloadResult(false, null, null, "Audio file not found after download");
                }
            } else {
                return new DownloadResult(false, null, null, "yt-dlp exited with code: " + exitCode);
            }

        } catch (Exception e) {
            return new DownloadResult(false, null, null, e.getMessage());
        }
    }

    private static @NotNull List<String> buildAudioCmd(String ytDlpPath, String url, String outputTemplate) {
        List<String> command = new ArrayList<>();
        command.add(ytDlpPath);
        command.add(url);
        command.add("-o");
        command.add(outputTemplate);
        command.add("-x");
        command.add("--audio-format");
        command.add("mp3");
        command.add("--audio-quality");
        command.add("0");
        command.add("--no-playlist");
        command.add("--progress");
        command.add("--ffmpeg-location");
        command.add(BaseEnv.ffmpegProvider.absoluteFFmpeg);
        return command;
    }

    private static int executeCommand(List<String> command, Consumer<String> onProgress) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (onProgress != null) {
                    onProgress.accept(line);
                }
            }
        }

        return process.waitFor();
    }

    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".webm", ".mkv", ".avi", ".mov"};
    private static final String[] AUDIO_EXTENSIONS = {".mp3", ".m4a", ".ogg", ".wav", ".flac"};

    private static @Nullable String findFileInDirectory(Path dir, String baseName, String[] extensions) {
        try (Stream<Path> filesList = Files.list(dir)){
            return filesList
                    .filter(p -> {
                        String fileName = p.getFileName().toString();
                        if (!fileName.startsWith(baseName)) return false;
                        String lower = fileName.toLowerCase();
                        for (String ext : extensions) {
                            if (lower.endsWith(ext)) return true;
                        }
                        return false;
                    })
                    .findFirst()
                    .map(Path::toString)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static @NotNull Path getDefaultOutputDir(String name) {
        return FMLLoader.getGamePath().resolve("config").resolve(NarutoLoading.MOD_ID + "-sources").resolve(name);
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }

    public record DownloadResult(boolean success, String videoPath, String audioPath, String errorMessage) {
        public boolean hasVideo() {
            return videoPath != null && !videoPath.isEmpty();
        }

        public boolean hasAudio() {
            return audioPath != null && !audioPath.isEmpty();
        }

        @Override
        public @NotNull String toString() {
            if (success) {
                return "DownloadResult{success=true, video=" + videoPath + ", audio=" + audioPath + "}";
            } else {
                return "DownloadResult{success=false, error=" + errorMessage + "}";
            }
        }
    }
}