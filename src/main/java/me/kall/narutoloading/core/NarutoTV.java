package me.kall.narutoloading.core;

import me.kall.narutoloading.app.ffmpeg.VideoArgReader;
import me.kall.narutoloading.core.executor.audio.AudioRestartExecutor;
import me.kall.narutoloading.core.executor.audio.NarutoAudioExecutor;
import me.kall.narutoloading.core.executor.video.AbstractVideoExecutor;
import me.kall.narutoloading.data.NarutoConfig;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class NarutoTV<FRAME, TEXTURE, LOCATION> {
    public @Nullable TEXTURE texture;
    public @Nullable LOCATION textureLocation;

    public @Nullable AbstractVideoExecutor<FRAME> videoExecutor;
    public @Nullable NarutoAudioExecutor audioExecutor;

    public @Nullable LifetimeController lifetime;

    public double fps;
    public double duration;

    public void init() {
        this.init(true);
    }

    public void init(boolean texture) {
        if (this.isRunnable()) {
            VideoArgReader videoArgReader = new VideoArgReader(this.absoluteVideoPath().get());
            this.fps = videoArgReader.fps();
            this.duration = videoArgReader.duration();

            this.createVideo();
            this.createAudio();
            this.createLifetime();

            if (texture) this.createTexture();

            this.setupVideo();
            this.setupAudio();
            this.setupLifetime();
        }
    }

    public void updateFrame() {
        if (!this.isRunnable()) return;
        if (this.texture == null) this.init();
        if (this.lifetime != null && this.videoExecutor != null && this.lifetime.shouldUpdateFrame(this.fps)) {
            FRAME frame = this.videoExecutor.fetch(this.lifetime.elapsedSeconds());
            if (frame != null) this.consumeFrame(frame, this.texture);
        }
    }

    public void renderFrame() {
        if (this.isRunnable()) {
            if (this.lifetime != null) {
                this.lifetime.syncSoundEngine();
                this.lifetime.lagSpikeRestart();
                this.lifetime.endRestart();
            }

            this.updateFrame();
            if (this.textureLocation == null) return;
            this.renderFrame(this.textureLocation);
        }
    }

    public void cleanup() {
        this.cleanup(true);
    }

    public void cleanup(boolean texture) {
        this.cleanupVideo();
        this.cleanupAudio();
        this.cleanupLifetime();
        if (texture) this.cleanupTexture();
    }

    public Supplier<String> absoluteVideoPath() {
        return () -> NarutoConfig.VIDEO;
    }

    public Supplier<String> absoluteAudioPath() {
        return () -> NarutoConfig.AUDIO;
    }

    public abstract void createVideo();

    public void setupVideo() {
        if (this.videoExecutor != null) this.videoExecutor.setup();
    }

    public void cleanupVideo() {
        if (this.videoExecutor != null) {
            this.videoExecutor.shutdown();
            this.videoExecutor = null;
        }
    }

    public void createAudio() {
        this.audioExecutor = new NarutoAudioExecutor(() -> () -> AudioRestartExecutor.schedule(this::cleanup, this::init), this.absoluteVideoPath(), this.absoluteAudioPath());
    }

    public void setupAudio() {
        if (this.audioExecutor != null) this.audioExecutor.setup();
    }

    public void cleanupAudio() {
        if (this.audioExecutor != null) {
            this.audioExecutor.shutdown();
            this.audioExecutor = null;
        }
    }

    public void restart() {
        this.cleanup();
        this.init();
    }

    public void restartAt(String seconds) {
        boolean hasVideo = this.videoExecutor != null;
        boolean hasAudio = this.audioExecutor != null;
        if (hasVideo) this.videoExecutor.shutdown();
        if (hasAudio) this.audioExecutor.shutdown();
        if (hasVideo) this.videoExecutor.setup(seconds);
        if (hasAudio) this.audioExecutor.setup(seconds);
    }

    public void createLifetime() {
        this.lifetime = new LifetimeController(this.duration, System.nanoTime(), () -> this::restart, () -> this::restartAt, () -> this.audioExecutor != null);
    }

    public void setupLifetime() {
        if (this.lifetime != null) this.lifetime.start();
    }

    public void cleanupLifetime() {
        if (this.lifetime != null) {
            this.lifetime.stop();
            this.lifetime = null;
        }
    }

    public abstract boolean isRunnable();
    public abstract void createTexture();
    public abstract void consumeFrame(FRAME frame, TEXTURE texture);
    public abstract void renderFrame(LOCATION textureLocation);
    public abstract void cleanupTexture();
}
