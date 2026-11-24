package me.kall.narutoloading.agent;

import me.kall.narutoloading.agent.bootstrap.EarlyVideo;
import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.playback.render.BufferFrameRenderer;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.playback.produce.audio.AudioProducer;
import me.kall.narutoloading.playback.produce.video.AbstractFrameProducer;
import me.kall.narutoloading.playback.produce.video.BufferFrameProducer;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EarlyVideoImpl implements EarlyVideo {
    private final VideoArgs video;
    private final BufferFrameProducer producer;
    private final BufferFrameRenderer renderer = new BufferFrameRenderer(false);
    private final AudioProducer audio;

    private final AtomicBoolean clockStarted = new AtomicBoolean();

    private volatile long startNanos = -1L;
    private volatile boolean closed;
    private long lastFrameIndex = -1L;

    private EarlyVideoImpl(String ffmpeg, VideoArgs video, AudioArgs audioArgs, float volume) {
        this.video = video;

        AudioProducer created = null;

        if (audioArgs != null && volume > 0F) {
            try {
                created = new AudioProducer(ffmpeg, volume, audioArgs);
                created.setup(0D);
            } catch (Throwable throwable) {
                System.err.println("[NarutoLoading Agent] Early audio disabled: " + throwable.getMessage());
                created = null;
            }
        }

        this.audio = created;

        this.producer = new BufferFrameProducer(ffmpeg, video);
        this.producer.withWarmListener(this::onWarm);
        this.producer.setup(0D);
        this.producer.eager();

        this.renderer.setup(video.scaledWidth(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()), video.scaledHeight(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()), 0D, false);
    }

    public static @NotNull EarlyVideo create(String ffmpeg, String videoPath, int width, int height, double fps, double duration, double startSec, String audioPath, int channels, int sampleRate, double audioStartSec, double audioDuration, float volume) {
        VideoArgs video = new VideoArgs(videoPath, fps, width, height, duration, startSec);
        AudioArgs audio = audioPath != null && channels > 0 && sampleRate > 0 ? new AudioArgs(audioPath, channels, sampleRate, audioStartSec, audioDuration) : null;

        return new EarlyVideoImpl(ffmpeg, video, audio, volume);
    }

    private void onWarm() {
        if (!this.clockStarted.compareAndSet(false, true)) return;

        this.startNanos = System.nanoTime();
        if (this.audio != null) this.audio.eager();
    }

    @Override
    public void present(boolean flipY) {
        if (this.closed) return;

        try {
            double position = this.positionSec();
            long frameIndex = (long) (position * this.video.fps());

            if (frameIndex != this.lastFrameIndex) {
                this.lastFrameIndex = frameIndex;

                AbstractFrameProducer.Frame<ByteBuffer> frame = this.producer.fetch(position);
                if (frame != null) this.renderer.update(this.video.scaledWidth(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()), this.video.scaledHeight(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()), frame.data());
            }

            this.renderer.render(flipY);
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading Agent] Early video disabled: " + throwable.getMessage());
            this.close();
        }
    }

    @Override
    public double positionSec() {
        long start = this.startNanos;
        return start < 0L ? 0D : (System.nanoTime() - start) / 1_000_000_000D;
    }

    @Override
    public double durationSec() {
        return this.video.duration();
    }

    @Override
    public boolean finished() {
        return this.closed || this.positionSec() >= this.video.duration();
    }

    @Override
    public void close() {
        if (this.closed) return;
        this.closed = true;

        if (this.audio != null) this.audio.shutdown();
        this.producer.shutdown();
        this.renderer.shutdown();
    }
}
