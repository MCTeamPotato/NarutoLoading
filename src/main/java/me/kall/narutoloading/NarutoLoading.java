package me.kall.narutoloading;

import me.kall.narutoloading.executor.NarutoAudioExecutor;
import me.kall.narutoloading.executor.NarutoVideoExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";

    public static final String FFMPEG_PATH = "D:\\ffmpeg\\bin\\ffmpeg.exe";

    public static final String VIDEO_PATH = FMLLoader.getGamePath().resolve("config").resolve("naruto.mp4").toAbsolutePath().toString();

    public static final int FPS = 30;

    public static final NarutoAudioExecutor AUDIO = new NarutoAudioExecutor();
    public static final NarutoVideoExecutor VIDEO = new NarutoVideoExecutor();

    public static final NarutoRenderer RENDERER = new NarutoRenderer();
}
