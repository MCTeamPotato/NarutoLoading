package me.kall.narutoloading.data;

import java.util.concurrent.atomic.AtomicReference;

public class NarutoConfig {
    public static final int RELOAD  = 301;
    public static final int WIDTH   = 1280;
    public static final int HEIGHT  = 720;

    private static final AtomicReference<String> VIDEO = new AtomicReference<>("");
    private static final AtomicReference<String> AUDIO = new AtomicReference<>("");

    static {
        init();
    }

    public static void init() {
        Sources.Source source = Sources.rollSource();
        if (source == null) throw new NullPointerException("Source not found");
        VIDEO.set(source.absoluteVideoPath());
        AUDIO.set(source.absoluteAudioPath());
    }

    public static String getVideo() {
        return VIDEO.get();
    }

    public static String getAudio() {
        return AUDIO.get();
    }
}