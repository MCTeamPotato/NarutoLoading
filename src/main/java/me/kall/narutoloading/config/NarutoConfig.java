package me.kall.narutoloading.config;

import me.kall.duplicationless.config.JsonConfig;
import me.kall.narutoloading.NarutoLoading;
import net.minecraftforge.fml.loading.FMLLoader;
import org.lwjgl.glfw.GLFW;

public final class NarutoConfig {
    private static final JsonConfig CONFIG = JsonConfig.create(NarutoLoading.MOD_ID, "5")
            .put("ffmpegExePath", "D:\\your\\ffmpeg\\file.exe")
            .put("ffprobeExePath", "D:\\your\\ffprobe\\file.exe")
            .put("ffmpegLinuxDownloadLink", "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linux64-lgpl.tar.xz")
            .put("ffmpegWindowsDownloadLink", "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-lgpl.zip")
            .put("videoFileName", "naruto.mp4")
            .put("audioFileName", "")
            .put("reloadKey", GLFW.GLFW_KEY_F12)
            .put("audioVolume", 1.0)
            .put("maxResolutionWidth", 1350)
            .put("maxResolutionHeight", 720)
            .put("videoFrameStorageBufferSize", 60)
            .put("logErrors", false)
            .initialize();

    public static final int RELOAD = CONFIG.getInt("reloadKey");

    private static final String VIDEO_PATH = FMLLoader.getGamePath().resolve("config").resolve(CONFIG.getString("videoFileName")).toAbsolutePath().toString();
    private static final String AUDIO_PATH = CONFIG.getString("audioFileName").isBlank() ? "" : FMLLoader.getGamePath().resolve("config").resolve(CONFIG.getString("audioFileName")).toAbsolutePath().toString();

    public static final String FFPROBE_PATH = CONFIG.getString("ffprobeExePath");
    public static final String FFMPEG_PATH = CONFIG.getString("ffmpegExePath");

    public static final int WIDTH = CONFIG.getInt("maxResolutionWidth");
    public static final int HEIGHT = CONFIG.getInt("maxResolutionHeight");

    public static final int BUFFER_SIZE = CONFIG.getInt("videoFrameStorageBufferSize");

    public static final float VOLUME = CONFIG.getFloat("audioVolume");

    public static final boolean DEBUG = CONFIG.getBoolean("logErrors");

    public static final String WIN_URL = CONFIG.getString("ffmpegWindowsDownloadLink");
    public static final String LINUX_URL = CONFIG.getString("ffmpegLinuxDownloadLink");

    public static String video() {
        return VIDEO_PATH;
    }

    public static String audio() {
        return AUDIO_PATH.isEmpty() ? video() : AUDIO_PATH;
    }

    public static void init() {
        NarutoLoading.LOGGER.info("[NarutoLoading] FFprobe path in Config: {}", FFPROBE_PATH);
        NarutoLoading.LOGGER.info("[NarutoLoading] FFmpeg path in Config: {}", FFMPEG_PATH);
    }
}
