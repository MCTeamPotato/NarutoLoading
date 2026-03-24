package me.kall.narutoloading.common.env;

import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.common.env.ffmpeg.FFmpegProvider;
import me.kall.narutoloading.common.env.ytdlp.YtDlpProvider;

public final class BaseEnv {
    private BaseEnv() {}

    private static NarutoConfig narutoConfig;
    private static volatile FFmpegProvider ffmpegProvider;
    private static volatile YtDlpProvider ytDlpProvider;

    private static volatile boolean available;

    public static boolean available() {
        return available;
    }

    public static void setupEnv(boolean roll) {
        narutoConfig = new NarutoConfig(roll);

        ffmpegProvider = new FFmpegProvider(getNarutoConfig().absoluteFFprobePath, getNarutoConfig().absoluteFFmpegPath);

         ytDlpProvider = new YtDlpProvider(getNarutoConfig().absoluteYtDlpPath);

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

    public static NarutoConfig getNarutoConfig() {
        if (narutoConfig == null) setupEnv(false);
        return narutoConfig;
    }

    public static FFmpegProvider getFfmpegProvider() {
        if (ffmpegProvider == null) setupEnv(false);
        return ffmpegProvider;
    }

    public static YtDlpProvider getYtDlpProvider() {
        if (ytDlpProvider == null) setupEnv(false);
        return ytDlpProvider;
    }
}