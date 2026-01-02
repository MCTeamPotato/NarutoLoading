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

    public static void setupEnv(boolean roll) {
        narutoConfig = new NarutoConfig(roll);

        ffmpegProvider = new FFmpegProvider(narutoConfig.absoluteFFprobePath, narutoConfig.absoluteFFmpegPath, narutoConfig.winUrl, narutoConfig.linuxUrl, narutoConfig.macUrl);

        ffmpegProvider.setup(() -> {
            if (ffmpegProvider.absoluteFFprobe == null) return;
            noWorldVideoArgs = new VideoArgReader(narutoConfig.absoluteVideoPath, ffmpegProvider.absoluteFFprobe);
            available = true;
            ffmpegProvider.shutdown();
        });
    }
}