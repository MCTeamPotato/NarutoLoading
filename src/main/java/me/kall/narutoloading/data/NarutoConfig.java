package me.kall.narutoloading.data;

public class NarutoConfig {
    public static final int RELOAD  = 301;
    public static final int WIDTH = 1280;
    public static final int HEIGHT  = 720;

    static {
        init();
    }

    public static void init() {
        if ("true".equals(System.getProperty("narutoconfig.initial"))) return;
        roll();
    }

    public static void roll() {
        Sources.Source source = Sources.rollSource();
        if (source == null) throw new NullPointerException("Source not found");
        System.setProperty("narutoconfig.video", source.absoluteVideoPath());
        System.setProperty("narutoconfig.audio", source.absoluteAudioPath());
        System.setProperty("narutoconfig.initial", "true");
    }

    public static String getVideo() {
        return System.getProperty("narutoconfig.video");
    }

    public static String getAudio() {
        return System.getProperty("narutoconfig.audio");
    }
}