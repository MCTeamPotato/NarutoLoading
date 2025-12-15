package me.kall.narutoloading.core;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.data.VideoArgs;
import me.kall.narutoloading.core.execution.NarutoAudioExecutor;
import me.kall.narutoloading.core.execution.NarutoVideoExecutor;

public final class LifetimeController {
    private long lastFrameTime = 0L;
    private long startTime = -1L;
    private long elapsedTime = 0L;
    private long frameCount = 0L;

    private boolean running = false;

    private boolean lagSpikeDetected = false;
    private int lagSpikeCooldown = 200;

    private volatile boolean syncSoundEngine = false;

    public void tick() {
        long now = System.currentTimeMillis();

        if (this.startTime == -1L) this.startTime = now;

        this.elapsedTime = now - this.startTime;
        this.frameCount++;
    }

    public void setSyncSoundEngine(boolean syncSoundEngine) {
        this.syncSoundEngine = syncSoundEngine;
    }

    public boolean shouldUpdateFrame(int fps) {
        long now = System.currentTimeMillis();
        if (now - this.lastFrameTime >= 1000L / fps) {
            this.lastFrameTime = now;
            return true;
        }
        return false;
    }

    public void detectLagSpike() {
        this.lagSpikeDetected = true;
    }

    private boolean shouldRestartForLag() {
        if (this.lagSpikeCooldown > 0) {
            this.lagSpikeCooldown--;
            this.lagSpikeDetected = false;
            return false;
        }

        if (this.lagSpikeDetected) {
            this.lagSpikeDetected = false;
            this.lagSpikeCooldown = 200;
            return true;
        }
        return false;
    }

    public void start() {
        this.running = true;
        this.startTime = -1L;
        this.elapsedTime = 0L;
        this.frameCount = 0L;
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

    public long frameCount() {
        return this.frameCount;
    }

    public void endRestart() {
        if (this.elapsedTime >= VideoArgs.duration()) {
            NarutoRenderer.INSTANCE.shutdown();
            NarutoRenderer.INSTANCE.setup();
        }
    }

    public void lagSpikeRestart() {
        if (this.shouldRestartForLag()) {
            String sec = String.valueOf(this.elapsedSeconds());
            NarutoLoading.LOGGER.warn("Lag spike detected, restarting video from {} seconds", sec);
            NarutoVideoExecutor.INSTANCE.shutdown(this.frameCount());
            NarutoVideoExecutor.INSTANCE.setup(sec);
        }
    }

    public void syncSoundEngine() {
        if (this.syncSoundEngine) {
            this.setSyncSoundEngine(false);
            NarutoAudioExecutor.INSTANCE.setup(String.valueOf(this.elapsedSeconds()));
        }
    }
}