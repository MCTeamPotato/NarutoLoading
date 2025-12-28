package me.kall.narutoloading.util;

public class VideoArgReader {
    private int fps = 0;
    private long duration = 0L;

    private final String video;
    private final String ffprobe;

    public VideoArgReader(String video, String ffprobe) {
        this.video = video;
        this.ffprobe = ffprobe;
        this.setup();
    }

    public void setup() {
        String json = FFprobe.genJson(this.video, this.ffprobe);
        this.fps = FFprobe.getFps(json);
        this.duration = FFprobe.getDuration(json);
    }

    public int fps() {
        return this.fps;
    }

    public long duration() {
        return this.duration;
    }
}
