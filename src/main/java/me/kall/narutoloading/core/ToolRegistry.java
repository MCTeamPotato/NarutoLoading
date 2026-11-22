package me.kall.narutoloading.core;

import me.kall.narutoloading.media.ffmpeg.FFmpeg;
import me.kall.narutoloading.media.ytdlp.YtDlp;
import me.kall.narutoloading.config.NarutoConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ToolRegistry {
    private static final String FFMPEG = "ffmpeg";
    private static final String FFPROBE = "ffprobe";
    private static final String YTDLP = "yt-dlp";

    public static final String YTDLP_FILE_NAME = Locator.WINDOWS ? YTDLP + ".exe" : YTDLP;
    public static final String YTDLP_FILE_FILTER = Locator.WINDOWS ? "*.exe" : YTDLP + "*";

    public final FFmpeg ffmpeg = new FFmpeg();
    public final YtDlp ytDlp = new YtDlp();

    public ToolRegistry() {
        try {
            Path gameDir = GamePaths.gameDir();
            Locator.Index index = new Locator.Index();

            this.ffmpeg.ffmpeg.set(Locator.locate(FFMPEG, gameDir, null, NarutoConfig.ffmpegDir(), index));
            this.ffmpeg.ffprobe.set(Locator.locate(FFPROBE, gameDir, this.ffmpeg.ffmpeg.get(), NarutoConfig.ffmpegDir(), index));
            this.ytDlp.ytdlp.set(Locator.locate(YTDLP, gameDir, null, NarutoConfig.ytDlpDir(), index));
        } catch (Exception exception) {
            System.err.println("[NarutoLoading] Exception locating FFmpeg and YtDlp paths: " + exception.getMessage());
        }
    }

    public @Nullable String reloadYtDlp() {
        String located = Locator.locate(YTDLP, GamePaths.gameDir(), null, NarutoConfig.ytDlpDir(), new Locator.Index());
        this.ytDlp.ytdlp.set(located);
        return located;
    }

    public void reloadFFmpeg() {
        Path gameDir = GamePaths.gameDir();
        Locator.Index index = new Locator.Index();

        this.ffmpeg.ffmpeg.set(Locator.locate(FFMPEG, gameDir, null, NarutoConfig.ffmpegDir(), index));
        this.ffmpeg.ffprobe.set(Locator.locate(FFPROBE, gameDir, this.ffmpeg.ffmpeg.get(), NarutoConfig.ffmpegDir(), index));
    }

    public @Nullable String installYtDlp(@NotNull Path source) {
        Path target = GamePaths.gameDir().resolve(YTDLP_FILE_NAME).toAbsolutePath();

        try {
            if (!source.toAbsolutePath().normalize().equals(target.normalize())) Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Exception installing yt-dlp from " + source + " to " + target + ": " + exception.getMessage());
            return null;
        }

        if (!Locator.WINDOWS && !target.toFile().setExecutable(true, false)) System.err.println("[NarutoLoading] Warning: " + target + " may not be executable.");

        return this.reloadYtDlp();
    }
}
