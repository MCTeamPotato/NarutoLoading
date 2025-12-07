package me.kall.narutoloading;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";

    public static final String FFMPEG_PATH = "D:\\ffmpeg\\bin\\ffmpeg.exe";

    public static final String VIDEO_PATH = FMLLoader.getGamePath().resolve("config").resolve("naruto.mp4").toAbsolutePath().toString();
}
