package me.kall.narutoloading.data;

import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public final class FFmpeg {
    public static final @Nullable String FFMPEG;
    public static final @Nullable String FFPROBE;

    static {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        File ffmpegFile = get(isWindows ? "ffmpeg.exe" : "ffmpeg");
        File ffprobeFile = get(isWindows ? "ffprobe.exe" : "ffprobe");
        FFMPEG = ffmpegFile.exists() ? ffmpegFile.getAbsolutePath() : null;
        FFPROBE = ffprobeFile.exists() ? ffprobeFile.getAbsolutePath() : null;
    }

    private static @NotNull File get(String fileName) {
        return FMLLoader.getGamePath().resolve("ffmpeg").resolve("bin").resolve(fileName).toFile();
    }
}