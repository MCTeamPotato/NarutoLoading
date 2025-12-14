package me.kall.narutoloading.data;

import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public final class FFmpeg {
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    private static final File FFMPEG_NAME = get(isWindows ? "ffmpeg.exe" : "ffmpeg");
    private static final File FFPROBE_NAME = get(isWindows ? "ffprobe.exe" : "ffprobe");

    public static final @Nullable String FFMPEG = FFMPEG_NAME.exists() ? FFMPEG_NAME.getAbsolutePath() : null;
    public static final @Nullable String FFPROBE = FFPROBE_NAME.exists() ? FFPROBE_NAME.getAbsolutePath() : null;

    private static @NotNull File get(String fileName) {
        return FMLLoader.getGamePath().resolve("ffmpeg").resolve("bin").resolve(fileName).toFile();
    }
}