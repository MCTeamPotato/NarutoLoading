package me.kall.narutoloading.media.download;

import me.kall.narutoloading.media.ytdlp.YtDlp;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public final class DownloadService {
    private final YtDlp ytDlp;
    private final ExecutorService download;
    private final DownloadState state = new DownloadState();

    public DownloadService(@NotNull YtDlp ytDlp, @NotNull ExecutorService download) {
        this.ytDlp = ytDlp;
        this.download = download;
    }

    public @NotNull DownloadState state() {
        return this.state;
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
