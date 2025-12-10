package me.kall.narutoloading.config;

import me.kall.duplicationless.config.JsonConfig;
import me.kall.narutoloading.NarutoLoading;
import net.minecraftforge.fml.loading.FMLLoader;
import org.lwjgl.glfw.GLFW;

public class NarutoConfig {
    private static final JsonConfig CONFIG = JsonConfig.create(NarutoLoading.MOD_ID, "2")
            .put("ffmpegExePath", "D:\\ffmpeg\\bin\\ffmpeg.exe")
            .put("ffprobeExePath", "D:\\ffmpeg\\bin\\ffprobe.exe")
            .put("videoFileName", "naruto.mp4")
            .put("audioFileName", "")
            .put("reloadKey", GLFW.GLFW_KEY_F12)
            .initialize();

    public static final int RELOAD = CONFIG.getInt("ReloadKey");

    private static final String VIDEO_PATH = FMLLoader.getGamePath().resolve("config").resolve(CONFIG.getString("videoFileName")).toAbsolutePath().toString();
    private static final String AUDIO_PATH = FMLLoader.getGamePath().resolve("config").resolve(CONFIG.getString("audioFileName")).toAbsolutePath().toString();

    public static final String FFPROBE_PATH = CONFIG.getString("ffprobeExePath");
    public static final String FFMPEG_PATH = CONFIG.getString("ffmpegExePath");

    public static String video() {
        return VIDEO_PATH;
    }

    public static String audio() {
        return !AUDIO_PATH.endsWith(".mp3") ? video() : AUDIO_PATH;
    }
}
