package me.kall.narutoloading.playback.produce;

import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.client.context.LevelStatus;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.playback.produce.audio.AudioProducer;
import me.kall.narutoloading.playback.produce.video.BufferFrameProducer;
import me.kall.narutoloading.playback.produce.video.ImageFrameProducer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NarutoProducers {
    private NarutoProducers() {}

    @Contract("_, _ -> new")
    public static @NotNull AudioProducer buildAudio(@Nullable String ffmpeg, AudioArgs audioArgs) {
        return new AudioProducer(ffmpeg, NarutoConfig.volume(), audioArgs, NarutoConfig.bufferSeconds());
    }

    @Contract("_, _ -> new")
    public static @NotNull BufferFrameProducer buildBuffer(@Nullable String ffmpeg, VideoArgs videoArgs) {
        return new BufferFrameProducer(ffmpeg, videoArgs, LevelStatus.notFound(), NarutoConfig.bufferSeconds());
    }

    @Contract("_, _ -> new")
    public static @NotNull ImageFrameProducer buildImage(@Nullable String ffmpeg, VideoArgs videoArgs) {
        return new ImageFrameProducer(ffmpeg, videoArgs, LevelStatus.notFound(), NarutoConfig.bufferSeconds());
    }

    @Contract("_, _ -> new")
    public static @NotNull ImageFrameProducer buildWorldImage(@Nullable String ffmpeg, VideoArgs videoArgs) {
        return new ImageFrameProducer(ffmpeg, videoArgs, false, NarutoConfig.worldBufferSeconds(), NarutoConfig.worldLimitWidth(), NarutoConfig.worldLimitHeight());
    }

    @Contract("_, _ -> new")
    public static @NotNull BufferFrameProducer buildWorldBuffer(@Nullable String ffmpeg, VideoArgs videoArgs) {
        return new BufferFrameProducer(ffmpeg, videoArgs, false, NarutoConfig.worldBufferSeconds(), NarutoConfig.worldLimitWidth(), NarutoConfig.worldLimitHeight());
    }
}
