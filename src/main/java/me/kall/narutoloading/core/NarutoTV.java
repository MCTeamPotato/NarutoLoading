package me.kall.narutoloading.core;

import me.kall.narutoloading.app.ffmpeg.VideoArgReader;
import me.kall.narutoloading.core.executor.audio.AbstractAudioExecutor;
import me.kall.narutoloading.core.executor.video.AbstractVideoExecutor;
import me.kall.narutoloading.data.NarutoConfig;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
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

    protected final ReentrantLock lifecycleLock = new ReentrantLock();

    public void init() {
        this.init(true);
    }

    public void init(boolean createTexture) {
        this.lifecycleLock.lock();
        try {
            this.doInit(createTexture);
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    public void cleanup() {
        this.cleanup(true);
    }

    public void cleanup(boolean destroyTexture) {
        this.lifecycleLock.lock();
        try {
            this.doCleanup(destroyTexture);
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    public void restart() {
        this.lifecycleLock.lock();
        try {
            this.doCleanup(true);
            this.doInit(true);
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    public void restartAt(String seconds) {
        this.lifecycleLock.lock();
        try {
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
        } finally {
            this.lifecycleLock.unlock();
        }
    }

    private void doInit(boolean createTexture) {
        if (!this.isRunnable()) return;

        VideoArgReader reader = new VideoArgReader(absoluteVideoPath().get());
        this.setFps(reader.fps());
        this.setDuration(reader.duration());

        this.createVideo();
        this.createAudio();
        if (createTexture) this.createTexture();

        AbstractVideoExecutor<FRAME> videoExecutor = this.videoExecutor.get();
        AbstractAudioExecutor audioExecutor = this.audioExecutor.get();
        if (videoExecutor != null) videoExecutor.setup();
        if (audioExecutor != null) audioExecutor.setup();

        LifetimeController lifetime = new LifetimeController(this.getDuration(), System.nanoTime(), () -> this::restart, () -> this::restartAt, () -> this.audioExecutor.get() != null);
        this.lifetime.set(lifetime);
        lifetime.start();
    }

    private void doCleanup(boolean destroyTexture) {
        AbstractVideoExecutor<FRAME> videoExecutor = this.videoExecutor.getAndSet(null);
        if (videoExecutor != null) videoExecutor.shutdown();

        AbstractAudioExecutor audioExecutor = this.audioExecutor.getAndSet(null);
        if (audioExecutor != null) audioExecutor.shutdown();

        LifetimeController lifetime = this.lifetime.getAndSet(null);
        if (lifetime != null) lifetime.stop();

        if (destroyTexture) this.cleanupTexture();
    }

    public void updateFrame() {
        if (!this.isRunnable()) return;

        if (this.texture.get() == null) {
            this.lifecycleLock.lock();
            try {
                if (this.texture.get() == null) {
                    this.doInit(true);
                }
            } finally {
                this.lifecycleLock.unlock();
            }
        }

        LifetimeController lifetime = this.lifetime.get();
        AbstractVideoExecutor<FRAME> videoExecutor = this.videoExecutor.get();
        TEXTURE texture = this.texture.get();

        if (lifetime != null && videoExecutor != null && texture != null && lifetime.shouldUpdateFrame(getFps())) {
            FRAME frame = videoExecutor.fetch(lifetime.elapsedSeconds());
            if (frame != null) consumeFrame(frame, texture);
        }
    }

    public void renderFrame() {
        if (!this.isRunnable()) return;

        LifetimeController lifetime = this.lifetime.get();
        if (lifetime != null) {
            lifetime.syncSoundEngine();
            lifetime.lagSpikeRestart();
            lifetime.endRestart();
        }

        this.updateFrame();

        LOCATION location = this.textureLocation.get();
        if (location != null) this.renderFrame(location);
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