package me.kall.narutoloading.common.env.config;

import me.kall.duplicationless.config.JsonConfig;
import me.kall.narutoloading.NarutoLoading;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

public final class NarutoConfig {
    private static final Logger LOGGER = LogManager.getLogger(NarutoConfig.class);

    public JsonConfig config;

    public int reload;

    public String videoFileName;
    public String audioFileName;

    public String absoluteFFprobePath;
    public String absoluteFFmpegPath;
    public String absoluteYtDlpPath;

    private int maxResolutionWidth;
    private int maxResolutionHeight;

    public int bufferSize;

    public float volume;

    public boolean debug;

    public String winUrl;
    public String linuxUrl;
    public String macUrl;

    public String ytdlpWinUrl;
    public String ytdlpLinuxUrl;
    public String ytdlpMacUrl;

    public String absoluteVideoPath;
    public String absoluteAudioPath;

    public boolean urlSource;

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

    public NarutoConfig(boolean roll) {
        this.init(roll);
    }

    public void init(boolean roll) {
        this.config = JsonConfig.create(NarutoLoading.MOD_ID, "7")
                .put("ffmpegExePath", "D:\\your\\ffmpeg\\file.exe")
                .put("ffprobeExePath", "D:\\your\\ffprobe\\file.exe")
                .put("ytdlpExePath", "D:\\your\\yt-dlp\\file.exe")
                .put("ffmpegLinuxDownloadLink", "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linux64-lgpl.tar.xz")
                .put("ffmpegWindowsDownloadLink", "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-lgpl.zip")
                .put("ffmpegMacDownloadLink", "https://evermeet.cx/ffmpeg")
                .put("enableUrlForSourceSelection", false)
                .put("ytdlpWindowsDownloadLink", "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe")
                .put("ytdlpLinuxDownloadLink", "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp")
                .put("ytdlpMacDownloadLink", "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_macos")
                .put("videoFileName", "naruto.mp4")
                .put("audioFileName", NarutoLoading.BLANK)
                .put("reloadKey", GLFW.GLFW_KEY_F12)
                .put("audioVolume", 1.0)
                .put("maxResolutionWidth", 1350)
                .put("maxResolutionHeight", 720)
                .put("videoFrameStorageBufferSize", 60)
                .put("logErrors", false)
                .initialize();

        this.reload = this.config.getInt("reloadKey");

        this.videoFileName = this.config.getString("videoFileName");
        this.absoluteVideoPath = NarutoConfig.absolute(this.videoFileName);

        this.audioFileName = this.config.getString("audioFileName");
        this.absoluteAudioPath = this.audioFileName.isBlank() ? NarutoLoading.BLANK : NarutoConfig.absolute(this.audioFileName);

        this.absoluteFFprobePath = this.config.getString("ffprobeExePath");
        this.absoluteFFmpegPath = this.config.getString("ffmpegExePath");
        this.absoluteYtDlpPath = this.config.getString("ytdlpExePath");

        this.maxResolutionWidth = this.config.getInt("maxResolutionWidth");
        this.maxResolutionHeight = this.config.getInt("maxResolutionHeight");

        this.bufferSize = this.config.getInt("videoFrameStorageBufferSize");

        this.volume = this.config.getFloat("audioVolume");

        this.debug = this.config.getBoolean("logErrors");

        this.winUrl = this.config.getString("ffmpegWindowsDownloadLink");
        this.linuxUrl = this.config.getString("ffmpegLinuxDownloadLink");
        this.macUrl = this.config.getString("ffmpegMacDownloadLink");

        this.urlSource = this.config.getBoolean("enableUrlForSourceSelection");

        this.ytdlpWinUrl = this.config.getString("ytdlpWindowsDownloadLink");
        this.ytdlpLinuxUrl = this.config.getString("ytdlpLinuxDownloadLink");
        this.ytdlpMacUrl = this.config.getString("ytdlpMacDownloadLink");

        if (roll) this.roll();

        this.log();
    }

    public void roll() {
        SourceCollector.Source source = SourceCollector.roll();
        if (source != null) {
            this.absoluteVideoPath = source.absoluteVideoPath();
            this.videoFileName = relative(this.absoluteVideoPath);

            this.absoluteAudioPath = source.absoluteAudioPath();
            this.audioFileName = relative(this.absoluteAudioPath);

            this.config.put("videoFileName", this.videoFileName).put("audioFileName", this.audioFileName).saveToFile();
        }
    }

    private void log() {
        LOGGER.info(this.toString());
    }

    public String toString() {
        return "NarutoConfig Arguments: {[Reload key: " + this.reload +
                "], [Video Path: " + this.videoFileName +
                "], [Audio Path: " + this.audioFileName +
                "], [FFprobe Path: " + this.absoluteFFprobePath +
                "], [FFmpeg Path:" + this.absoluteFFmpegPath +
                "], [yt-dlp Path:" + this.absoluteYtDlpPath +
                "], [Max Resolution Width: " + this.maxResolutionWidth +
                "], [Max Resolution Height: " + this.maxResolutionHeight +
                "], [Video Frame Storage Buffer Size: " + this.bufferSize +
                "], [Audio Volume: " + this.volume +
                "], [Log Errors Or Not: " + this.debug +
                "], [Enable Url ForSource Selection: " + this.urlSource +
                "], [FFmpeg Windows Download Link: " + this.winUrl +
                "], [FFmpeg Linux Download Link:" + this.linuxUrl +
                "], [FFmpeg Mac Download Link:" + this.macUrl +
                "], [yt-dlp Windows Download Link: " + this.ytdlpWinUrl +
                "], [yt-dlp Linux Download Link:" + this.ytdlpLinuxUrl +
                "], [yt-dlp Mac Download Link:" + this.ytdlpMacUrl + "]}";
    }

    public static @NotNull String absolute(@NotNull String relativePath) {
        if (relativePath.isBlank()) return NarutoLoading.BLANK;
        return FMLLoader.getGamePath().resolve("config").resolve(relativePath).toAbsolutePath().toString();
    }

    public static @NotNull String relative(@NotNull String absolutePath) {
        if (absolutePath.isBlank()) return NarutoLoading.BLANK;
        Path configPath = FMLLoader.getGamePath().resolve("config").toAbsolutePath().normalize();
        Path targetPath = Path.of(absolutePath).toAbsolutePath().normalize();
        if (!targetPath.startsWith(configPath)) return NarutoLoading.BLANK;
        return configPath.relativize(targetPath).toString();
    }
}