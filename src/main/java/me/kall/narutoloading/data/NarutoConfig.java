package me.kall.narutoloading.data;

import me.kall.duplicationless.config.JsonConfig;
import me.kall.narutoloading.NarutoLoading;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class NarutoConfig {
    private static final Logger LOGGER = LogManager.getLogger(NarutoConfig.class);

    public static JsonConfig config;

    public static int reload;

    public static String videoName;
    public static String audioName;

    public static String ffprobePath;
    public static String ffmpegPath;

    public static int width;
    public static int height;

    public static int bufferSize;

    public static float volume;

    public static boolean debug;

    public static String winUrl;
    public static String linuxUrl;

    private static String videoPath;
    private static String audioPath;

    public static @NotNull String video() {
        return videoPath;
    }

    public static @NotNull String audio() {
        return audioPath;
    }

    public static void init() {
        config = JsonConfig.create(NarutoLoading.MOD_ID, "5")
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

        reload = config.getInt("reloadKey");

        videoName = config.getString("videoFileName");
        audioName = config.getString("audioFileName");

        videoPath = FMLLoader.getGamePath().resolve("config").resolve(videoName).toAbsolutePath().toString();
        audioPath = audioName.isBlank() ? "" : FMLLoader.getGamePath().resolve("config").resolve(audioName).toAbsolutePath().toString();

        ffprobePath = config.getString("ffprobeExePath");
        ffmpegPath = config.getString("ffmpegExePath");

        width = config.getInt("maxResolutionWidth");
        height = config.getInt("maxResolutionHeight");

        bufferSize = config.getInt("videoFrameStorageBufferSize");

        volume = config.getFloat("audioVolume");

        debug = config.getBoolean("logErrors");

        winUrl = config.getString("ffmpegWindowsDownloadLink");
        linuxUrl = config.getString("ffmpegLinuxDownloadLink");

        LOGGER.info("Reload key in NarutoConfig: {}", reload);

        LOGGER.info("Video path in NarutoConfig: {}", videoName);
        LOGGER.info("Audio path in NarutoConfig: {}", audioName);

        LOGGER.info("FFprobe path in NarutoConfig: {}", ffprobePath);
        LOGGER.info("FFmpeg path in NarutoConfig: {}", ffmpegPath);

        LOGGER.info("Max resolution width in NarutoConfig: {}", width);
        LOGGER.info("Max resolution height in NarutoConfig: {}", height);

        LOGGER.info("Video frame storage buffer size in NarutoConfig: {}", bufferSize);

        LOGGER.info("Audio volume in NarutoConfig: {}", volume);

        LOGGER.info("Log errors in NarutoConfig: {}", debug);

        LOGGER.info("FFmpeg Windows download link in NarutoConfig: {}", winUrl);
        LOGGER.info("FFmpeg Linux download link in NarutoConfig: {}", linuxUrl);

        FFmpeg.init();
    }
}
