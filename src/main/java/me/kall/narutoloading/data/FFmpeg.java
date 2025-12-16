package me.kall.narutoloading.data;

import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public final class FFmpeg {
    public static final @Nullable String FFMPEG;
    public static final @Nullable String FFPROBE;

    static {
        OSType os = OSType.current();

        Path gamePath = FMLLoader.getGamePath();
        String baseDir = getBase(gamePath, os);

        if (baseDir == null) {
            FFMPEG = null;
            FFPROBE = null;
        } else {
            boolean windows = os == OSType.WINDOWS;

            String ffmpegName = windows ? "ffmpeg.exe" : "ffmpeg";
            String ffprobeName = windows ? "ffprobe.exe" : "ffprobe";

            File ffmpegFile = get(baseDir, ffmpegName);
            File ffprobeFile = get(baseDir, ffprobeName);

            FFMPEG = ffmpegFile.exists() ? ffmpegFile.getAbsolutePath() : null;
            FFPROBE = ffprobeFile.exists() ? ffprobeFile.getAbsolutePath() : null;
        }
    }

    private static @Nullable String getBase(Path gamePath, @Nullable OSType os) {
        if (os != null) {
            Path osDir = gamePath.resolve(os.dirName);
            if (osDir.toFile().exists()) {
                return os.dirName;
            }
        }

        return gamePath.resolve("ffmpeg").toFile().exists() ? "ffmpeg" : null;
    }

    private static @NotNull File get(String baseDir, String fileName) {
        return FMLLoader.getGamePath().resolve(baseDir).resolve("bin").resolve(fileName).toFile();
    }

    private enum OSType {
        WINDOWS("ffmpeg-win"),
        LINUX("ffmpeg-linux"),
        MAC("ffmpeg-mac");

        final String dirName;

        OSType(String dirName) {
            this.dirName = dirName;
        }

        static @Nullable OSType current() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) return WINDOWS;
            if (os.contains("linux")) return LINUX;
            if (os.contains("mac") || os.contains("darwin")) return MAC;
            return null;
        }
    }
}
