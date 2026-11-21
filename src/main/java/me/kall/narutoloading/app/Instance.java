package me.kall.narutoloading.app;

import me.kall.narutoloading.app.ffmpeg.FFmpeg;
import me.kall.narutoloading.app.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.app.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.app.ytdlp.YtDlp;
import me.kall.narutoloading.config.NarutoConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class Instance {
    private static final String FFMPEG = "ffmpeg";
    private static final String FFPROBE = "ffprobe";
    private static final String YTDLP = "yt-dlp";

    public static final String YTDLP_FILE_NAME = Locator.WINDOWS ? YTDLP + ".exe" : YTDLP;
    public static final String YTDLP_FILE_FILTER = Locator.WINDOWS ? "*.exe" : YTDLP + "*";

    private static final Instance INSTANCE = new Instance();

    private final ExecutorService probe;
    private final ExecutorService download;

    public final FFmpeg ffmpeg;
    public final YtDlp ytDlp;

    public final MediaRepository media = new MediaRepository(NarutoConfig::sourcesDir, NarutoConfig::selectMode, NarutoConfig::fixedName, this::readVideo, this::readAudio);

    private Instance() {
        this.ffmpeg = new FFmpeg();
        this.ytDlp = new YtDlp();

        this.probe = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoProbe");
            thread.setDaemon(true);
            return thread;
        });

        this.download = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoDownload");
            thread.setDaemon(true);
            return thread;
        });

        try {
            Path gameDir = Path.of(System.getProperty("user.dir", "."));
            Locator.Index index = new Locator.Index();

            this.ffmpeg.ffmpeg.set(Locator.locate(FFMPEG, gameDir, null, NarutoConfig.ffmpegDir(), index));
            this.ffmpeg.ffprobe.set(Locator.locate(FFPROBE, gameDir, this.ffmpeg.ffmpeg.get(), NarutoConfig.ffmpegDir(), index));
            this.ytDlp.ytdlp.set(Locator.locate(YTDLP, gameDir, null, NarutoConfig.ytDlpDir(), index));
        } catch (Exception exception) {
            System.err.println("[NarutoLoading] Exception locating FFmpeg and YtDlp paths: " + exception.getMessage());
        }
    }

    public static Instance get() {
        return INSTANCE;
    }

    public @Nullable String reloadYtDlp() {
        String located = Locator.locate(YTDLP, Path.of(System.getProperty("user.dir", ".")), null, NarutoConfig.ytDlpDir(), new Locator.Index());
        this.ytDlp.ytdlp.set(located);
        return located;
    }

    public void reloadFFmpeg() {
        Path gameDir = Path.of(System.getProperty("user.dir", "."));
        Locator.Index index = new Locator.Index();

        this.ffmpeg.ffmpeg.set(Locator.locate(FFMPEG, gameDir, null, NarutoConfig.ffmpegDir(), index));
        this.ffmpeg.ffprobe.set(Locator.locate(FFPROBE, gameDir, this.ffmpeg.ffmpeg.get(), NarutoConfig.ffmpegDir(), index));

        this.media.invalidate();
    }

    public @Nullable String installYtDlp(@NotNull Path source) {
        Path target = Path.of(System.getProperty("user.dir", ".")).resolve(YTDLP_FILE_NAME).toAbsolutePath();

        try {
            if (!source.toAbsolutePath().normalize().equals(target.normalize())) Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Exception installing yt-dlp from " + source + " to " + target + ": " + exception.getMessage());
            return null;
        }

        if (!Locator.WINDOWS && !target.toFile().setExecutable(true, false)) System.err.println("[NarutoLoading] Warning: " + target + " may not be executable.");

        return this.reloadYtDlp();
    }

    @Contract("_ -> new")
    public @NotNull CompletableFuture<@Nullable VideoArgs> readVideo(@Nullable String absPathStr) {
        return CompletableFuture.supplyAsync(() -> this.ffmpeg.deriveVideo(absPathStr), this.probe);
    }

    @Contract("_ -> new")
    public @NotNull CompletableFuture<@Nullable AudioArgs> readAudio(@Nullable String absPathStr) {
        return CompletableFuture.supplyAsync(() -> this.ffmpeg.deriveAudio(absPathStr), this.probe);
    }

    @Contract("_ -> new")
    public @NotNull CompletableFuture<@Nullable String> monoize(@Nullable String absPathStr) {
        return CompletableFuture.supplyAsync(() -> this.ffmpeg.monoize(absPathStr), this.probe);
    }

    @Contract("_, _, _, _ -> new")
    public @NotNull CompletableFuture<@Nullable String> downloadVideo(String url, Path outputDirectory, String name, @Nullable Consumer<String> progress) {
        return CompletableFuture.supplyAsync(() -> this.ytDlp.downloadVideo(url, outputDirectory, name, progress), this.download);
    }

    @Contract("_, _, _, _ -> new")
    public @NotNull CompletableFuture<@Nullable String> downloadAudio(String url, Path outputDirectory, String name, @Nullable Consumer<String> progress) {
        return CompletableFuture.supplyAsync(() -> this.ytDlp.downloadAudio(url, outputDirectory, name, progress), this.download);
    }
}
