package me.kall.narutoloading.produce;

import me.kall.narutoloading.app.Instance;
import me.kall.narutoloading.app.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.app.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.client.context.LevelStatus;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.produce.audio.AudioProducer;
import me.kall.narutoloading.produce.video.BufferFrameProducer;
import me.kall.narutoloading.produce.video.ImageFrameProducer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class NarutoProducers {
    private NarutoProducers() {}

    @Contract("_ -> new")
    public static @NotNull AudioProducer buildAudio(AudioArgs audioArgs) {
        return new AudioProducer(Instance.get().ffmpeg.ffmpeg.get(), NarutoConfig.volume(), audioArgs, NarutoConfig.bufferSeconds());
    }

    @Contract("_ -> new")
    public static @NotNull BufferFrameProducer buildBuffer(VideoArgs videoArgs) {
        return new BufferFrameProducer(Instance.get().ffmpeg.ffmpeg.get(), videoArgs, LevelStatus.notFound(), NarutoConfig.bufferSeconds());
    }

    @Contract("_ -> new")
    public static @NotNull ImageFrameProducer buildImage(VideoArgs videoArgs) {
        return new ImageFrameProducer(Instance.get().ffmpeg.ffmpeg.get(), videoArgs, LevelStatus.notFound(), NarutoConfig.bufferSeconds());
    }
}
