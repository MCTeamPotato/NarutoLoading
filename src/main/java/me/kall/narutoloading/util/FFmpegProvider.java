package me.kall.narutoloading.util;

import me.kall.narutoloading.NarutoLoading;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FFmpegProvider {
    public volatile @Nullable String ffmpeg;
    public volatile @Nullable String ffprobe;

    private final String ffprobePath, ffmpegPath, winUrl, linuxUrl;

    private final ExecutorService downloader;

    public FFmpegProvider(String ffprobePath, String ffmpegPath, String winUrl, String linuxUrl) {
        this.ffprobePath = ffprobePath;
        this.ffmpegPath = ffmpegPath;
        this.winUrl = winUrl;
        this.linuxUrl = linuxUrl;
        this.downloader = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task , "NarutoFFmpegDownloader");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void setup(Runnable onDone) {
        String ffprobePath = Files.validExe(this.ffprobePath);
        String ffmpegPath = Files.validExe(this.ffmpegPath);

        if (!ffprobePath.isBlank() && !ffmpegPath.isBlank()) {
            this.ffprobe = ffprobePath;
            this.ffmpeg = ffmpegPath;
            NarutoLoading.LOGGER.info("Using FFmpeg from config.");
            onDone.run();
            return;
        }

        OSType os = OSType.CURRENT;
        Path gamePath = FMLLoader.getGamePath();

        boolean windows = os == OSType.WINDOWS;
        String ffmpegName = windows ? "ffmpeg.exe" : "ffmpeg";
        String ffprobeName = windows ? "ffprobe.exe" : "ffprobe";

        this.downloader.submit(() -> {
            String baseDir = OSType.getBase(gamePath, os);

            if (baseDir == null && os != null) {
                try {
                    NarutoLoading.LOGGER.info("Downloading FFmpeg...");
                    Downloader.download(gamePath, os, os.equals(OSType.WINDOWS) ? this.winUrl : this.linuxUrl);
                    baseDir = OSType.getBase(gamePath, os);
                } catch (Exception exception) {
                    NarutoLoading.LOGGER.error("Error downloading FFmpeg.", exception);
                } finally {
                    NarutoLoading.LOGGER.info("FFmpeg download task ends.");
                }
            }

            if (baseDir == null) {
                this.ffmpeg = null;
                this.ffprobe = null;
                onDone.run();
                return;
            }

            File ffmpegFile = Files.getExe(baseDir, ffmpegName);
            File ffprobeFile = Files.getExe(baseDir, ffprobeName);

            this.ffmpeg = ffmpegFile.exists() ? ffmpegFile.getAbsolutePath() : null;
            this.ffprobe = ffprobeFile.exists() ? ffprobeFile.getAbsolutePath() : null;
            onDone.run();
        });
    }

    public void shutdown() {
        this.downloader.shutdownNow();
    }
}
