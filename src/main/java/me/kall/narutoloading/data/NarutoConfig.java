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

    public JsonConfig config;

    public int reload;

    public String videoName;
    public String audioName;

    public String ffprobePath;
    public String ffmpegPath;

    public int width;
    public int height;

    public int bufferSize;

    public float volume;

    public boolean debug;

    public String winUrl;
    public String linuxUrl;

    public String video;
    public String audio;

    public static @NotNull String toPath(String name) {
        return FMLLoader.getGamePath().resolve("config").resolve(name).toAbsolutePath().toString();
    }

    public NarutoConfig() {
        this.init();
    }

    public void init() {
        this.config = JsonConfig.create(NarutoLoading.MOD_ID, "5")
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

        this.reload = this.config.getInt("reloadKey");

        this.videoName = this.config.getString("videoFileName");
        this.video = NarutoConfig.toPath(this.videoName);

        this.audioName = this.config.getString("audioFileName");
        this.audio = this.audioName.isBlank() ? "" : NarutoConfig.toPath(this.audioName);

        this.ffprobePath = this.config.getString("ffprobeExePath");
        this.ffmpegPath = this.config.getString("ffmpegExePath");

        this.width = this.config.getInt("maxResolutionWidth");
        this.height = this.config.getInt("maxResolutionHeight");

        this.bufferSize = this.config.getInt("videoFrameStorageBufferSize");

        this.volume = this.config.getFloat("audioVolume");

        this.debug = this.config.getBoolean("logErrors");

        this.winUrl = this.config.getString("ffmpegWindowsDownloadLink");
        this.linuxUrl = this.config.getString("ffmpegLinuxDownloadLink");

        this.log();
    }

    private void log() {
        LOGGER.info("Reload key in NarutoConfig: {}", this.reload);

        LOGGER.info("Video path in NarutoConfig: {}", this.videoName);
        LOGGER.info("Audio path in NarutoConfig: {}", this.audioName);

        LOGGER.info("FFprobe path in NarutoConfig: {}", this.ffprobePath);
        LOGGER.info("FFmpeg path in NarutoConfig: {}", this.ffmpegPath);

        LOGGER.info("Max resolution width in NarutoConfig: {}", this.width);
        LOGGER.info("Max resolution height in NarutoConfig: {}", this.height);

        LOGGER.info("Video frame storage buffer size in NarutoConfig: {}", this.bufferSize);

        LOGGER.info("Audio volume in NarutoConfig: {}", this.volume);

        LOGGER.info("Log errors in NarutoConfig: {}", this.debug);

        LOGGER.info("FFmpeg Windows download link in NarutoConfig: {}", this.winUrl);
        LOGGER.info("FFmpeg Linux download link in NarutoConfig: {}", this.linuxUrl);
    }
}
