package me.kall.narutoloading.common;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.noworld.core.NarutoRenderer;

public final class LifetimeController {
    private long lastFrameTime = 0L;
    private long startTime = -1L;
    private long elapsedTime = 0L;

    private boolean running = false;

    public volatile boolean lagSpikeDetected = false;

    public volatile boolean syncSoundEngine = false;

    private final NarutoRenderer renderer;
    private final long duration;

    public LifetimeController(NarutoRenderer renderer, long duration) {
        this.renderer = renderer;
        this.duration = duration;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (this.startTime == -1L) this.startTime = now;
        this.elapsedTime = now - this.startTime;
    }

    public boolean shouldUpdateFrame(int fps) {
        long now = System.currentTimeMillis();
        if (now - this.lastFrameTime >= 1000L / fps) {
            this.lastFrameTime = now;
            return true;
        }
        return false;
    }

    public void start() {
        this.running = true;
        this.startTime = -1L;
        this.elapsedTime = 0L;
        this.lastFrameTime = 0L;
    }

    public void stop() {
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    public double elapsedSeconds() {
        return (double) this.elapsedTime / 1000D;
    }

    public void endRestart() {
        if (this.elapsedTime >= this.duration) {
            this.renderer.shutdown();
            this.renderer.setup();
        }
    }

    public void lagSpikeRestart() {
        if (this.lagSpikeDetected) {
            this.lagSpikeDetected = false;
            String sec = String.valueOf(this.elapsedSeconds());
            NarutoLoading.LOGGER.warn("Lag spike detected, restarting video from {} seconds", sec);
            this.renderer.videoExecutor.shutdown((long) (this.elapsedSeconds() * this.duration));
            this.renderer.videoExecutor.setup(sec);
        }
    }

    public void syncSoundEngine() {
        if (this.syncSoundEngine) {
            this.syncSoundEngine = false;
            this.renderer.audioExecutor.setup(String.valueOf(this.elapsedSeconds()));
        }
    }
}