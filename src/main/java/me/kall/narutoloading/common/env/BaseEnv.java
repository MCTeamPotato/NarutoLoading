package me.kall.narutoloading.common.env;

import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.common.env.ffmpeg.FFmpegProvider;
import me.kall.narutoloading.common.env.ffmpeg.VideoArgReader;
import me.kall.narutoloading.common.env.ytdlp.YtDlpProvider;

public final class BaseEnv {
    private BaseEnv() {}

    public static NarutoConfig narutoConfig;
    public static volatile FFmpegProvider ffmpegProvider;
    public static volatile YtDlpProvider ytDlpProvider;
    public static volatile VideoArgReader noWorldVideoArgs;

    private static volatile boolean available;

    public static boolean available() {
        return available;
    }

    public static void setupEnv(boolean roll) {
        narutoConfig = new NarutoConfig(roll);

        ffmpegProvider = new FFmpegProvider(narutoConfig.absoluteFFprobePath, narutoConfig.absoluteFFmpegPath, narutoConfig.winUrl, narutoConfig.linuxUrl, narutoConfig.macUrl);

        if (narutoConfig.urlSource) ytDlpProvider = new YtDlpProvider(narutoConfig.absoluteYtDlpPath, narutoConfig.ytdlpWinUrl, narutoConfig.ytdlpLinuxUrl, narutoConfig.ytdlpMacUrl);

        ffmpegProvider.setup(() -> {
            if (ffmpegProvider.absoluteFFmpeg != null) {
                if (narutoConfig.urlSource) {
                    ytDlpProvider.setup(() -> ytDlpProvider.shutdown());
                }

                noWorldVideoArgs = new VideoArgReader(narutoConfig.absoluteVideoPath, ffmpegProvider.absoluteFFprobe);
                available = true;

                if (narutoConfig.urlSource) ytDlpProvider.setup(() -> ytDlpProvider.shutdown());
            }

            ffmpegProvider.shutdown();
        });
    }
}