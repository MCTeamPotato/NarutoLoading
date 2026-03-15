package me.kall.narutoloading.common.env;

import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.common.env.ffmpeg.FFmpegProvider;
import me.kall.narutoloading.common.env.ytdlp.YtDlpProvider;

public final class BaseEnv {
    private BaseEnv() {}

    public static NarutoConfig narutoConfig;
    public static volatile FFmpegProvider ffmpegProvider;
    public static volatile YtDlpProvider ytDlpProvider;

    private static volatile boolean available;

    public static boolean available() {
        return available;
    }

    public static void setupEnv(boolean roll) {
        narutoConfig = new NarutoConfig(roll);

        ffmpegProvider = new FFmpegProvider(narutoConfig.absoluteFFprobePath, narutoConfig.absoluteFFmpegPath);

        if (narutoConfig.urlSource) ytDlpProvider = new YtDlpProvider(narutoConfig.absoluteYtDlpPath);

        ffmpegProvider.setup(() -> {
            if (ffmpegProvider.absoluteFFmpeg != null) {
                if (narutoConfig.urlSource) {
                    ytDlpProvider.setup(() -> {
                        if (ytDlpProvider.absoluteYtDlp != null) available = true;
                    });
                } else {
                    available = true;
                }
            }
        });
    }
}