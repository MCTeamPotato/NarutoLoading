package me.kall.narutoloading.playback.produce;

import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.client.context.LevelStatus;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.playback.produce.audio.AudioProducer;
import me.kall.narutoloading.playback.produce.video.BufferFrameProducer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NarutoProducers {
    private static final long WORLD_MAX_BUFFER_BYTES = 256L * 1024L * 1024L;

    private static final int WORLD_BUFFER_FRAMES = 8;

    private NarutoProducers() {}

    @Contract("_, _ -> new")
    public static @NotNull AudioProducer buildAudio(@Nullable String ffmpeg, AudioArgs audioArgs) {
        return new AudioProducer(ffmpeg, NarutoConfig.volume(), audioArgs);
    }

    @Contract("_, _ -> new")
    public static @NotNull BufferFrameProducer buildBuffer(@Nullable String ffmpeg, VideoArgs videoArgs) {
        return new BufferFrameProducer(ffmpeg, videoArgs, LevelStatus.notFound());
    }

    @Contract("_, _ -> new")
    public static @NotNull BufferFrameProducer buildWorldBuffer(@Nullable String ffmpeg, VideoArgs videoArgs) {
        return new BufferFrameProducer(ffmpeg, videoArgs, false, NarutoConfig.worldLimitWidth(), NarutoConfig.worldLimitHeight(), WORLD_MAX_BUFFER_BYTES, WORLD_BUFFER_FRAMES);
    }
}
