package me.kall.narutoloading.app.ffmpeg;

public class VideoArgReader {
    private final double fps;
    private final double duration;

    public VideoArgReader(String absoluteVideoPath) {
        String json = FFmpeg.genJson(absoluteVideoPath);
        this.fps = FFmpeg.getFps(json);
        this.duration = FFmpeg.getDuration(json);
    }

    public double fps() {
        return this.fps;
    }

    public double duration() {
        return this.duration;
    }
}
