package me.kall.narutoloading.common.env;

import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.util.FFmpegProvider;
import me.kall.narutoloading.util.VideoArgReader;

public final class BaseEnv {
    private BaseEnv() {}

    public static NarutoConfig narutoConfig;
    public static volatile FFmpegProvider ffmpegProvider;
    public static volatile VideoArgReader videoArgReader;

    private static volatile boolean available;

    public static boolean available() {
        return available;
    }

    public static void setupEnv() {
        narutoConfig = new NarutoConfig();

        ffmpegProvider = new FFmpegProvider(narutoConfig.ffprobePath, narutoConfig.ffmpegPath, narutoConfig.winUrl, narutoConfig.linuxUrl);
        ffmpegProvider.setup(() -> {
            ffmpegProvider.shutdown();

            if (ffmpegProvider.ffprobe == null) return;
            videoArgReader = new VideoArgReader(narutoConfig.video, ffmpegProvider.ffprobe);
            available = true;
        });
    }
}
