package me.kall.narutoloading.playback;

import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.playback.render.FrameRenderer;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.playback.produce.audio.AudioProducer;
import me.kall.narutoloading.playback.produce.util.TimeLine;
import me.kall.narutoloading.playback.produce.video.AbstractFrameProducer;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractTV<T> {
    private static final long VIDEO_WARM_TIMEOUT_NANOS = 3_000_000_000L;

    public final AtomicReference<@Nullable AbstractFrameProducer<T>> video = new AtomicReference<>();
    public final AtomicReference<@Nullable AudioProducer> audio = new AtomicReference<>();

    public final AtomicReference<@Nullable TimeLine> timeLine = new AtomicReference<>();

    public final AtomicReference<@Nullable CompletableFuture<@Nullable AbstractFrameProducer<T>>> nextVideoTask = new AtomicReference<>();
    public final AtomicReference<@Nullable CompletableFuture<@Nullable AudioProducer>> nextAudioTask = new AtomicReference<>();

    public final FrameRenderer<T> renderer;

    private final AtomicBoolean running = new AtomicBoolean();

    private final AtomicBoolean videoDelayed = new AtomicBoolean();

    private final AtomicLong setupNanos = new AtomicLong();

    public AbstractTV(FrameRenderer<T> renderer) {
        this.renderer = renderer;
    }

    public synchronized boolean isRunning() {
        return this.running.get();
    }

    public abstract boolean prerequisite();

    public abstract @Nullable VideoArgs newVideoArgs();
    public abstract @Nullable AudioArgs newAudioArgs();

    public abstract @Nullable AbstractFrameProducer<T> createVideo(@Nullable VideoArgs videoArgs);
    public abstract @Nullable AudioProducer createAudio(@Nullable AudioArgs audioArgs);

    protected @Nullable TimeLine newTimeLine(@Nullable VideoArgs videoArgs, @Nullable AudioArgs audioArgs, double seekTo) {
        if (videoArgs == null && audioArgs == null) return null;

        TimeLine timeLine = videoArgs != null ? TimeLine.video(videoArgs.duration(), videoArgs.fps()) : TimeLine.audio(audioArgs.duration());

        return timeLine.withPauseListener(this::pauseAudio).withResumeListener(this::resumeAudio).seekTo(seekTo);
    }

    private void pauseAudio() {
        AudioProducer audio = this.audio.get();
        if (audio != null) audio.shutdown();
    }

    private void resumeAudio(double at) {
        AudioProducer audio = this.audio.get();
        if (audio != null) audio.setup(at);
    }

    private @Nullable AbstractFrameProducer<T> buildVideo(@Nullable VideoArgs videoArgs) {
        if (videoArgs == null) return null;
        AbstractFrameProducer<T> video = this.createVideo(videoArgs);
        return video == null ? null : video.withWarmListener(() -> this.startClock(video, null));
    }

    private @Nullable AudioProducer buildAudio(@Nullable AudioArgs audioArgs) {
        if (audioArgs == null) return null;
        AudioProducer audio = this.createAudio(audioArgs);
        return audio == null ? null : audio.withWarmListener(() -> this.startClock(null, audio));
    }

    private void startClock(@Nullable AbstractFrameProducer<T> video, @Nullable AudioProducer audio) {
        if (video != null) {
            if (this.video.get() != video) return;
        } else {
            if (this.video.get() != null && !this.videoDelayed.get()) return;
            if (audio == null || this.audio.get() != audio) return;
        }

        TimeLine timeLine = this.timeLine.get();
        if (timeLine == null) return;

        timeLine.start();

        if (video != null) {
            AudioProducer current = this.audio.get();
            if (current != null) current.eager();
        }
    }

    public synchronized void setup(double seekTo) {
        if (!this.prerequisite()) return;

        this.videoDelayed.set(false);
        this.setupNanos.set(System.nanoTime());

        CompletableFuture<@Nullable AbstractFrameProducer<T>> nextVideoTask = this.nextVideoTask.getAndSet(null);
        AbstractFrameProducer<T> nextVideo = nextVideoTask != null ? nextVideoTask.join() : null;

        CompletableFuture<@Nullable AudioProducer> nextAudioTask = this.nextAudioTask.getAndSet(null);
        AudioProducer nextAudio = nextAudioTask != null ? nextAudioTask.join() : null;

        AbstractFrameProducer<T> video = this.video.get();
        AudioProducer audio = this.audio.get();

        VideoArgs videoArgs = video == null ? this.newVideoArgs() : video.args;
        AudioArgs audioArgs = audio == null ? this.newAudioArgs() : audio.args;

        TimeLine timeLine = this.newTimeLine(videoArgs, audioArgs, seekTo);
        this.timeLine.set(timeLine);

        AbstractFrameProducer<T> newVideo = nextVideo != null ? nextVideo : this.buildVideo(videoArgs);
        AudioProducer newAudio = nextAudio != null ? nextAudio : this.buildAudio(audioArgs);

        boolean warmVideo = newVideo != null && !newVideo.off.get();

        if (newVideo != null && !warmVideo) newVideo.setup(seekTo);
        if (newAudio != null && newAudio.off.get()) newAudio.setup(seekTo);

        if (newVideo != null && newVideo.off.get()) {
            newVideo.shutdown();
            newVideo = null;
        }

        if (newAudio != null && newAudio.off.get()) {
            newAudio.shutdown();
            newAudio = null;
        }

        this.video.set(newVideo);
        this.audio.set(newAudio);

        if (newVideo != null) {
            int width = newVideo.args.scaledWidth(NarutoConfig.limitWidth(), NarutoConfig.limitHeight());
            int height = newVideo.args.scaledHeight(NarutoConfig.limitWidth(), NarutoConfig.limitHeight());

            this.renderer.setup(width, height, seekTo, warmVideo);

            newVideo.eager();

            AbstractFrameProducer.Frame<T> frame = newVideo.fetch(timeLine == null ? seekTo : timeLine.positionSec());
            this.renderer.update(width, height, frame == null ? null : frame.data());
        }

        this.running.set(newVideo != null || newAudio != null);
    }

    public synchronized void tick() {
        if (!this.isRunning()) this.setup(0D);

        AbstractFrameProducer<T> video = this.video.get();
        AudioProducer audio = this.audio.get();
        TimeLine timeLine = this.timeLine.get();

        if (video != null) video.eager();

        if (audio != null && video != null && timeLine != null && !timeLine.isStarted() && !this.videoDelayed.get() && System.nanoTime() - this.setupNanos.get() >= VIDEO_WARM_TIMEOUT_NANOS) {
            this.videoDelayed.set(true);
            System.err.println("[NarutoLoading] Video is not ready after " + (VIDEO_WARM_TIMEOUT_NANOS / 1_000_000_000L) + "s, letting audio start the clock.");
        }

        if (audio != null && (video == null || this.videoDelayed.get() || (timeLine != null && timeLine.isStarted()))) audio.eager();

        if (timeLine == null) return;

        if (timeLine.expired()) {
            this.synchronize(false, 0D);
            return;
        }

        if (timeLine.tick()) {
            System.err.println("[NarutoLoading] Lag spike is detected. Synchronizing...");
            this.synchronize(true, timeLine.positionSec());
            return;
        }

        if (!timeLine.isStarted()) return;

        if (video != null) {
            if (timeLine.nearEnd()) this.nextVideoTask.compareAndSet(null, CompletableFuture.supplyAsync(() -> this.buildVideo(video.args)));

            if (timeLine.due()) {
                AbstractFrameProducer.Frame<T> frame = video.fetch(timeLine.positionSec());

                if (video.consumeStarvation()) timeLine.detectLagSpike();

                this.renderer.update(video.args.scaledWidth(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()), video.args.scaledHeight(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()), frame == null ? null : frame.data());
            }

            this.renderer.render();
        }

        if (audio != null && timeLine.nearEnd()) this.nextAudioTask.compareAndSet(null, CompletableFuture.supplyAsync(() -> this.buildAudio(audio.args)));
    }

    public synchronized void shutdown(boolean next) {
        this.running.set(false);
        this.videoDelayed.set(false);

        AbstractFrameProducer<T> video = this.video.getAndSet(null);
        AudioProducer audio = this.audio.getAndSet(null);

        this.timeLine.set(null);
        this.renderer.shutdown();

        if (video != null) video.shutdown();
        if (audio != null) audio.shutdown();

        if (next) {
            CompletableFuture<@Nullable AbstractFrameProducer<T>> nextVideoTask = this.nextVideoTask.getAndSet(null);
            CompletableFuture<@Nullable AudioProducer> nextAudioTask = this.nextAudioTask.getAndSet(null);

            if (nextVideoTask != null) {
                nextVideoTask.thenAccept(nextVideo -> {
                    if (nextVideo != null) nextVideo.shutdown();
                });
            }

            if (nextAudioTask != null) {
                nextAudioTask.thenAccept(nextAudio -> {
                    if (nextAudio != null) nextAudio.shutdown();
                });
            }
        }
    }

    public synchronized void synchronize(boolean next, double seekTo) {
        this.shutdown(next);
        this.setup(seekTo);
    }
}