package me.kall.narutoloading.media;

import me.kall.narutoloading.media.ffmpeg.FFmpeg;
import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class ProbeService {
    private final FFmpeg ffmpeg;
    private final ExecutorService probe;

    public ProbeService(@NotNull FFmpeg ffmpeg, @NotNull ExecutorService probe) {
        this.ffmpeg = ffmpeg;
        this.probe = probe;
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
}
