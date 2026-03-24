package me.kall.narutoloading.common.env.config;

import me.kall.narutoloading.common.util.JsonConfig;
import me.kall.narutoloading.common.util.Paths;
import org.lwjgl.glfw.GLFW;

public final class NarutoConfig {
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

    public String absoluteVideoPath;
    public String absoluteAudioPath;

    public boolean urlSource;

    public int width() {
        return this.maxResolutionWidth;
    }

    public int height() {
        return this.maxResolutionHeight;
    }

    public NarutoConfig(boolean roll) {
        this.init(roll);
    }

    public void init(boolean roll) {
        this.config = JsonConfig.create("narutoloading", "8")
                .put("ffmpegExePath", "D:\\your\\ffmpeg\\file.exe")
                .put("ffprobeExePath", "D:\\your\\ffprobe\\file.exe")
                .put("ytdlpExePath", "D:\\your\\yt-dlp\\file.exe")
                .put("enableUrlForSourceSelection", false)
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
        this.absoluteVideoPath = Paths.absolute(this.videoFileName);

        this.audioFileName = this.config.getString("audioFileName");
        this.absoluteAudioPath = this.audioFileName.isBlank() ? "" : Paths.absolute(this.audioFileName);

        this.absoluteFFprobePath = this.config.getString("ffprobeExePath");
        this.absoluteFFmpegPath = this.config.getString("ffmpegExePath");
        this.absoluteYtDlpPath = this.config.getString("ytdlpExePath");

        this.maxResolutionWidth = this.config.getInt("maxResolutionWidth");
        this.maxResolutionHeight = this.config.getInt("maxResolutionHeight");

        this.bufferSize = this.config.getInt("videoFrameStorageBufferSize");

        this.volume = this.config.getFloat("audioVolume");

        this.debug = this.config.getBoolean("logErrors");

        this.urlSource = this.config.getBoolean("enableUrlForSourceSelection");

        if (roll) this.roll();
    }

    public void roll() {
        SourceCollector.Source source = SourceCollector.roll();
        if (source != null) {
            this.absoluteVideoPath = source.absoluteVideoPath();
            this.videoFileName = Paths.relative(this.absoluteVideoPath);

            this.absoluteAudioPath = source.absoluteAudioPath();
            this.audioFileName = Paths.relative(this.absoluteAudioPath);

            this.config.put("videoFileName", this.videoFileName).put("audioFileName", this.audioFileName).saveToFile();
        }
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
                "], [Enable Url For Source Selection: " + this.urlSource + "]}";
    }
}
