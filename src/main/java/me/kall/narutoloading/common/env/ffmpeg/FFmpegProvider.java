package me.kall.narutoloading.common.env.ffmpeg;

import me.kall.narutoloading.NarutoLoading;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public final class FFmpegProvider {
    public volatile @Nullable String absoluteFFmpeg;
    public volatile @Nullable String absoluteFFprobe;

    private final String uncheckedAbsoluteFFprobePath, uncheckedAbsoluteFFmpegPath;

    public FFmpegProvider(String uncheckedAbsoluteFFprobePath, String uncheckedAbsoluteFFmpegPath) {
        this.uncheckedAbsoluteFFprobePath = uncheckedAbsoluteFFprobePath;
        this.uncheckedAbsoluteFFmpegPath = uncheckedAbsoluteFFmpegPath;
    }

    public void setup(Runnable onDone) {
        String absoluteFFprobePath = Executable.validExe(this.uncheckedAbsoluteFFprobePath);
        String absoluteFFmpegPath = Executable.validExe(this.uncheckedAbsoluteFFmpegPath);

        if (!absoluteFFprobePath.isBlank() && !absoluteFFmpegPath.isBlank()) {
            this.absoluteFFprobe = absoluteFFprobePath;
            this.absoluteFFmpeg = absoluteFFmpegPath;
            NarutoLoading.LOGGER.info("{}Using FFmpeg from config.", NarutoLoading.info());
            onDone.run();
            return;
        }

        OSType os = OSType.CURRENT;
        Path gamePath = FMLLoader.getCurrent().getGameDir();

        boolean windows = os == OSType.WINDOWS;
        String ffmpegName = windows ? "ffmpeg.exe" : "ffmpeg";
        String ffprobeName = windows ? "ffprobe.exe" : "ffprobe";

        String baseDir = OSType.getBase(gamePath, os);

        if (baseDir == null) {
            NarutoLoading.LOGGER.warn("{}FFmpeg not found. Please set ffmpegExePath and ffprobeExePath in the config.", NarutoLoading.info());
            this.absoluteFFmpeg = null;
            this.absoluteFFprobe = null;
            onDone.run();
            return;
        }

        File ffmpegFile = Executable.getExe(baseDir, ffmpegName, os);
        File ffprobeFile = Executable.getExe(baseDir, ffprobeName, os);

        this.absoluteFFmpeg = ffmpegFile.exists() ? ffmpegFile.getAbsolutePath() : null;
        this.absoluteFFprobe = ffprobeFile.exists() ? ffprobeFile.getAbsolutePath() : null;
        NarutoLoading.LOGGER.info("{}NarutoLoading ffmpeg file path: {}", NarutoLoading.info(), this.absoluteFFmpeg);
        NarutoLoading.LOGGER.info("{}NarutoLoading ffprobe file path: {}", NarutoLoading.info(), this.absoluteFFprobe);
        onDone.run();
    }

    static class Executable {
        static @NotNull String validExe(String path) {
            if (path == null || path.isBlank()) return NarutoLoading.BLANK;
            File file = new File(path);
            return file.exists() ? file.getAbsolutePath() : NarutoLoading.BLANK;
        }

        static @NotNull File getExe(String baseDir, String fileName, OSType os) {
            if (os == OSType.MACOS) {
                return FMLLoader.getCurrent().getGameDir().resolve(baseDir).resolve(fileName).toFile();
            }
            return FMLLoader.getCurrent().getGameDir().resolve(baseDir).resolve("bin").resolve(fileName).toFile();
        }
    }

    enum OSType {
        WINDOWS("ffmpeg-win"),
        LINUX("ffmpeg-linux"),
        MACOS("ffmpeg-mac");

        static final OSType CURRENT = current();

        final String dirName;

        OSType(String dirName) {
            this.dirName = dirName;
        }

        static @Nullable FFmpegProvider.OSType current() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) return WINDOWS;
            if (os.contains("mac")) return MACOS;
            if (os.contains("linux")) return LINUX;
            return null;
        }

        static @Nullable String getBase(Path gamePath, @Nullable FFmpegProvider.OSType os) {
            if (os != null) {
                Path osDir = gamePath.resolve(os.dirName);
                if (osDir.toFile().exists()) {
                    return os.dirName;
                }
            }

            return gamePath.resolve("ffmpeg").toFile().exists() ? "ffmpeg" : null;
        }
    }
}