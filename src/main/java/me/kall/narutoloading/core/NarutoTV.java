package me.kall.narutoloading.core;

import me.kall.narutoloading.app.ffmpeg.VideoArgReader;
import me.kall.narutoloading.core.executor.audio.AbstractAudioExecutor;
import me.kall.narutoloading.core.executor.video.AbstractVideoExecutor;
import me.kall.narutoloading.data.NarutoConfig;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class NarutoTV<FRAME, TEXTURE, LOCATION> {
    protected final AtomicReference<TEXTURE> texture = new AtomicReference<>();
    protected final AtomicReference<LOCATION> textureLocation = new AtomicReference<>();
    protected final AtomicReference<AbstractVideoExecutor<FRAME>> videoExecutor = new AtomicReference<>();
    protected final AtomicReference<AbstractAudioExecutor> audioExecutor = new AtomicReference<>();
    protected final AtomicReference<LifetimeController> lifetime = new AtomicReference<>();

    private final AtomicLong fpsBits = new AtomicLong(Double.doubleToRawLongBits(0.0));
    private final AtomicLong durationBits = new AtomicLong(Double.doubleToRawLongBits(0.0));

    protected double getFps() {
        return Double.longBitsToDouble(this.fpsBits.get());
    }

    protected double getDuration() {
        return Double.longBitsToDouble(this.durationBits.get());
    }

    private void setFps(double fps) {
        this.fpsBits.set(Double.doubleToRawLongBits(fps));
    }

    private void setDuration(double duration) {
        this.durationBits.set(Double.doubleToRawLongBits(duration));
    }

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
            this.doCleanup(true);
            this.doInit(true);
        }
    }

    public void restartAt(String seconds) {
        synchronized (this) {
            AbstractVideoExecutor<FRAME> videoExecutor = this.videoExecutor.get();
            AbstractAudioExecutor audioExecutor = this.audioExecutor.get();
            if (videoExecutor != null) {
                videoExecutor.shutdown();
                videoExecutor.setup(seconds);
            }
            if (audioExecutor != null) {
                audioExecutor.shutdown();
                audioExecutor.setup(seconds);
            }
        }
    }

    private void doInit(boolean reuseTexture) {
        if (!this.isRunnable()) return;

        VideoArgReader reader = new VideoArgReader(absoluteVideoPath().get());
        this.setFps(reader.fps());
        this.setDuration(reader.duration());

        this.createVideo();
        this.createAudio();
        if (!reuseTexture) this.createTexture();

        AbstractVideoExecutor<FRAME> videoExecutor = this.videoExecutor.get();
        AbstractAudioExecutor audioExecutor = this.audioExecutor.get();
        if (videoExecutor != null) videoExecutor.setup();
        if (audioExecutor != null) audioExecutor.setup();

        LifetimeController lifetime = new LifetimeController(this.getDuration(), System.nanoTime(), () -> this::restart, () -> this::restartAt, () -> this.audioExecutor.get() != null);
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

        if (lifetime.shouldUpdateFrame(getFps())) {
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

    public abstract boolean isRunnable();
    public abstract void createVideo();
    public abstract void createAudio();
    public abstract void createTexture();
    public abstract void consumeFrame(FRAME frame, TEXTURE texture);
    public abstract void renderFrame(LOCATION textureLocation);
    public abstract void cleanupTexture();
}