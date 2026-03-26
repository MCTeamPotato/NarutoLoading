package me.kall.narutoloading.data;

public class NarutoConfig {
    public static final int RELOAD = 301;

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    public static String VIDEO;
    public static String AUDIO;

    static {
        init();
    }

    public static void init() {
        Sources.Source source = Sources.rollSource();
        if (source == null) throw new NullPointerException("Source not found");
        VIDEO = source.absoluteVideoPath();
        AUDIO = source.absoluteAudioPath();
    }
}
