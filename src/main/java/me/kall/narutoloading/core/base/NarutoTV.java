package me.kall.narutoloading.core.base;

import me.kall.narutoloading.app.ffmpeg.VideoArgReader;
import me.kall.narutoloading.core.executor.audio.AbstractAudioExecutor;
import me.kall.narutoloading.core.executor.video.AbstractVideoExecutor;
import me.kall.narutoloading.data.NarutoConfig;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class NarutoTV<FRAME, TEXTURE, LOCATION> extends VideoArgContainer {
    public final AtomicReference<TEXTURE> texture = new AtomicReference<>();
    public final AtomicReference<LOCATION> textureLocation = new AtomicReference<>();

    public final AtomicReference<AbstractVideoExecutor<FRAME>> videoExecutor = new AtomicReference<>();
    public final AtomicReference<AbstractAudioExecutor> audioExecutor = new AtomicReference<>();
    public final AtomicReference<LifetimeController> lifetime = new AtomicReference<>();

    public void init(boolean reuseTexture) {
        synchronized (this) {
            this.doInit(reuseTexture);
        }
    }

    public void cleanup(boolean reuseTexture) {
        synchronized (this) {
            this.doCleanup(reuseTexture);
        }
    }

    public void restart() {
        synchronized (this) {
            this.doCleanup(false);
            this.doInit(false);
        }
    }

    public void restartAt(String seconds) {
        synchronized (this) {
            if (this.texture.get() == null) this.doInit(false);

            AbstractVideoExecutor<FRAME> videoExecutor = this.videoExecutor.get();
            AbstractAudioExecutor audioExecutor = this.audioExecutor.get();
            LifetimeController lifetime = this.lifetime.get();
            boolean hasVideo = videoExecutor != null;
            boolean hasAudio = audioExecutor != null;
            if (hasAudio) audioExecutor.shutdown();
            if (hasVideo) videoExecutor.shutdown();
            if (hasAudio) audioExecutor.setup(seconds);
            if (hasVideo) videoExecutor.setup(seconds);
            if (lifetime != null) lifetime.seekTo(Double.parseDouble(seconds));
        }
    }

    private void doInit(boolean reuseTexture) {
        if (!this.isRunnable()) return;

        VideoArgReader reader = new VideoArgReader(this.absoluteVideoPath().get());
        this.setFps(reader.fps());
        this.setDuration(reader.duration());

        this.createVideo();
        this.createAudio();
        if (!reuseTexture) this.createTexture();

        LifetimeController lifetime = new LifetimeController(this.getDuration(), System.nanoTime(), () -> () -> {
            synchronized (this) {
                this.doCleanup(true);
                NarutoConfig.roll();
                this.doInit(true);
            }
        }, () -> this::restartAt, () -> this.audioExecutor.get() != null);
        this.lifetime.set(lifetime);
        lifetime.start();
    }

    private void doCleanup(boolean reuseTexture) {
        AbstractVideoExecutor<FRAME> videoExecutor = this.videoExecutor.getAndSet(null);
        if (videoExecutor != null) videoExecutor.shutdown();

        AbstractAudioExecutor audioExecutor = this.audioExecutor.getAndSet(null);
        if (audioExecutor != null) audioExecutor.shutdown();

        LifetimeController lifetime = this.lifetime.getAndSet(null);
        if (lifetime != null) lifetime.stop();

        if (!reuseTexture) this.cleanupTexture();
    }

    public void renderFrame() {
        if (!this.isRunnable()) return;

        synchronized (this) {
            if (this.texture.get() == null) {
                this.doInit(false);
            }
        }

        LifetimeController lifetime = this.lifetime.get();
        AbstractVideoExecutor<FRAME> videoExecutor = this.videoExecutor.get();
        TEXTURE texture = this.texture.get();
        LOCATION location = this.textureLocation.get();

        if (lifetime == null || videoExecutor == null || texture == null || location == null) {
            throw new RuntimeException("Error occurs during NarutoTV initialization. Lifetime: " + lifetime + ". Video Executor: " + videoExecutor + ". Texture: " + texture + ". Texture Location: " + location);
        }

        lifetime.syncSoundEngine();
        lifetime.lagSpikeRestart();
        lifetime.endRestart();

        if (lifetime.shouldUpdateFrame(this.getFps())) {
            FRAME frame = videoExecutor.fetch(lifetime.elapsedSeconds());
            if (frame != null) this.consumeFrame(frame, texture);
        }

        this.renderFrame(location);
    }

    public Supplier<String> absoluteVideoPath() {
        return NarutoConfig::getVideo;
    }

    public Supplier<String> absoluteAudioPath() {
        return NarutoConfig::getAudio;
    }

    public boolean isRunning() {
        LifetimeController lifetime = this.lifetime.get();
        if (lifetime == null) return false;
        return lifetime.isRunning();
    }

    public abstract boolean isRunnable();
    public abstract void createVideo();
    public abstract void createAudio();
    public abstract void createTexture();
    public abstract void consumeFrame(FRAME frame, TEXTURE texture);
    public abstract void renderFrame(LOCATION textureLocation);
    public abstract void cleanupTexture();
}