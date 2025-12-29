package me.kall.narutoloading.common.env.config;

import me.kall.duplicationless.config.JsonConfig;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.SourceCollector;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class NarutoConfig {
    private static final Logger LOGGER = LogManager.getLogger(NarutoConfig.class);

    public JsonConfig config;

    public int reload;

    public String videoFileName;
    public String audioFileName;

    public String absoluteFFprobePath;
    public String absoluteFFmpegPath;

    private int maxResolutionWidth;
    private int maxResolutionHeight;

    public int bufferSize;

    public float volume;

    public boolean debug;

    public String winUrl;
    public String linuxUrl;

    public String absoluteVideoPath;
    public String absoluteAudioPath;

    public int width() {
        int width = Minecraft.getInstance().getWindow().getScreenWidth();
        if (width > this.maxResolutionWidth) width = this.maxResolutionWidth;
        return width;
    }

    public int height() {
        int height = Minecraft.getInstance().getWindow().getScreenHeight();
        if (height > this.maxResolutionHeight) height = this.maxResolutionHeight;
        return height;
    }

    public @NotNull String widthString() {
        return String.valueOf(width());
    }

    public @NotNull String heightString() {
        return String.valueOf(height());
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

        this.videoFileName = this.config.getString("videoFileName");
        this.absoluteVideoPath = NarutoConfig.toPath(this.videoFileName);

        this.audioFileName = this.config.getString("audioFileName");
        this.absoluteAudioPath = this.audioFileName.isBlank() ? "" : NarutoConfig.toPath(this.audioFileName);

        this.absoluteFFprobePath = this.config.getString("ffprobeExePath");
        this.absoluteFFmpegPath = this.config.getString("ffmpegExePath");

        this.maxResolutionWidth = this.config.getInt("maxResolutionWidth");
        this.maxResolutionHeight = this.config.getInt("maxResolutionHeight");

        this.bufferSize = this.config.getInt("videoFrameStorageBufferSize");

        this.volume = this.config.getFloat("audioVolume");

        this.debug = this.config.getBoolean("logErrors");

        this.winUrl = this.config.getString("ffmpegWindowsDownloadLink");
        this.linuxUrl = this.config.getString("ffmpegLinuxDownloadLink");

        SourceCollector.Source source = SourceCollector.roll();
        if (source != null) {
            this.absoluteVideoPath = source.absoluteVideoPath();
            this.absoluteAudioPath = source.absoluteAudioPath();
        }

        this.log();
    }

    private void log() {
        LOGGER.info("Reload key in NarutoConfig: {}", this.reload);

        LOGGER.info("Video path in NarutoConfig: {}", this.videoFileName);
        LOGGER.info("Audio path in NarutoConfig: {}", this.audioFileName);

        LOGGER.info("FFprobe path in NarutoConfig: {}", this.absoluteFFprobePath);
        LOGGER.info("FFmpeg path in NarutoConfig: {}", this.absoluteFFmpegPath);

        LOGGER.info("Max resolution width in NarutoConfig: {}", this.maxResolutionWidth);
        LOGGER.info("Max resolution height in NarutoConfig: {}", this.maxResolutionHeight);

        LOGGER.info("Video frame storage buffer size in NarutoConfig: {}", this.bufferSize);

        LOGGER.info("Audio volume in NarutoConfig: {}", this.volume);

        LOGGER.info("Log errors in NarutoConfig: {}", this.debug);

        LOGGER.info("FFmpeg Windows download link in NarutoConfig: {}", this.winUrl);
        LOGGER.info("FFmpeg Linux download link in NarutoConfig: {}", this.linuxUrl);
    }

    public static @NotNull String toPath(@NotNull String name) {
        if (name.isBlank()) return "";
        return FMLLoader.getGamePath().resolve("config").resolve(name).toAbsolutePath().toString();
    }
}
