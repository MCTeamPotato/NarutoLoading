package me.kall.narutoloading.common.env;

import me.kall.narutoloading.common.env.config.NarutoConfig;

public final class BaseEnv {
    private BaseEnv() {}

    public static NarutoConfig narutoConfig;
    public static volatile FFmpegProvider ffmpegProvider;
    public static volatile VideoArgReader noWorldVideoArgs;

    private static volatile boolean available;

    public static boolean available() {
        return available;
    }

    public static void setupEnv() {
        narutoConfig = new NarutoConfig();

        ffmpegProvider = new FFmpegProvider(narutoConfig.ffprobePath, narutoConfig.ffmpegPath, narutoConfig.winUrl, narutoConfig.linuxUrl);
        ffmpegProvider.setup(() -> {
            if (ffmpegProvider.ffprobe == null) return;
            noWorldVideoArgs = new VideoArgReader(narutoConfig.video, ffmpegProvider.ffprobe);
            available = true;
        });
    }
}
