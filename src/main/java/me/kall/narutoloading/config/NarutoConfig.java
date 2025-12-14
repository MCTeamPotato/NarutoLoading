package me.kall.narutoloading.config;

import me.kall.duplicationless.config.JsonConfig;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.data.FFmpeg;
import net.minecraftforge.fml.loading.FMLLoader;
import org.lwjgl.glfw.GLFW;

public final class NarutoConfig {
    private static final JsonConfig CONFIG = JsonConfig.create(NarutoLoading.MOD_ID, "5")
            .put("ffmpegExePath", FFmpeg.FFMPEG != null ? FFmpeg.FFMPEG : "D:\\ffmpeg\\bin\\ffmpeg.exe")
            .put("ffprobeExePath", FFmpeg.FFPROBE != null ? FFmpeg.FFPROBE : "D:\\ffmpeg\\bin\\ffprobe.exe")
            .put("videoFileName", "naruto.mp4")
            .put("audioFileName", "")
            .put("reloadKey", GLFW.GLFW_KEY_F12)
            .put("audioVolume", 1.0)
            .put("maxResolutionWidth", 1350)
            .put("maxResolutionHeight", 720)
            .put("videoFrameStorageBufferSize", 60)
            .initialize();

    public static final int RELOAD = CONFIG.getInt("reloadKey");

    private static final String VIDEO_PATH = FMLLoader.getGamePath().resolve("config").resolve(CONFIG.getString("videoFileName")).toAbsolutePath().toString();
    private static final String AUDIO_PATH = CONFIG.getString("audioFileName").isBlank() ? "" : FMLLoader.getGamePath().resolve("config").resolve(CONFIG.getString("audioFileName")).toAbsolutePath().toString();

    public static final String FFPROBE_PATH = FFmpeg.FFPROBE != null ? FFmpeg.FFPROBE : CONFIG.getString("ffprobeExePath");
    public static final String FFMPEG_PATH = FFmpeg.FFMPEG != null ? FFmpeg.FFMPEG : CONFIG.getString("ffmpegExePath");

    static {
        NarutoLoading.LOGGER.info("[NarutoLoading] FFprobe path: {}", FFPROBE_PATH);
        NarutoLoading.LOGGER.info("[NarutoLoading] FFmpeg path: {}", FFMPEG_PATH);
    }

    public static final int WIDTH = CONFIG.getInt("maxResolutionWidth");
    public static final int HEIGHT = CONFIG.getInt("maxResolutionHeight");

    public static final int BUFFER_SIZE = CONFIG.getInt("videoFrameStorageBufferSize");

    public static final float VOLUME = CONFIG.getFloat("audioVolume");

    public static String video() {
        return VIDEO_PATH;
    }

    public static String audio() {
        return AUDIO_PATH.isEmpty() ? video() : AUDIO_PATH;
    }
}
