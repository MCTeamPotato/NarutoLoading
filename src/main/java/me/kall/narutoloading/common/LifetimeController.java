package me.kall.narutoloading.common;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;

public class LifetimeController {
    public volatile long absoluteSetupTime;
    private long lastFetchFrameTime = -1;

    private boolean running = false;

    public volatile boolean lagSpikeDetected = false;
    private long lastLagSpikeRestart = -1;

    public volatile boolean syncSoundEngine = false;

    protected NarutoRenderer renderer;
    private final long duration;

    public LifetimeController(NarutoRenderer renderer, long duration, long absoluteSetupTime) {
        this.renderer = renderer;
        this.duration = duration;
        this.absoluteSetupTime = absoluteSetupTime;
    }

    public boolean shouldUpdateFrame(int fps) {
        long now = System.nanoTime();

        if (this.lastFetchFrameTime == -1L) {
            this.lastFetchFrameTime = now;
            return true;
        }

        double intervalNanos = (double) now - (double) this.lastFetchFrameTime;
        if (intervalNanos >= (1_000_000_000.0 / (double) fps)) {
            this.lastFetchFrameTime = now;
            return true;
        }
        return false;
    }

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    public double elapsedSeconds() {
        return ((double)this.elapsedMillis()) / 1000.0D;
    }

    public long elapsedMillis() {
        return (System.nanoTime() - this.absoluteSetupTime) / 1_000_000L;
    }

    public void endRestart() {
        if (this.elapsedMillis() >= this.duration) {
            BaseEnv.setupEnv(true);
            this.renderer.shutdown();
            this.renderer.setup();
        }
    }

    public void lagSpikeRestart() {
        if (this.lagSpikeDetected) {
            this.lagSpikeDetected = false;
            if (this.lastLagSpikeRestart == -1) {
                this.lastLagSpikeRestart = System.nanoTime();
                return;
            }

            if (System.nanoTime() - this.lastLagSpikeRestart > 2_000_000_000L) {
                NarutoLoading.LOGGER.warn("Lag spike detected, restarting video and audio from {} seconds", this.elapsedSeconds());
                this.restart();
                this.lastLagSpikeRestart = System.nanoTime();
            }
        }
    }

    public void syncSoundEngine() {
        if (this.syncSoundEngine) {
            this.syncSoundEngine = false;
            if (this.renderer.audioExecutor != null) {
                this.restart();
                NarutoLoading.LOGGER.info("{}Syncing audio to {} seconds after sound engine reload", NarutoLoading.info(), this.elapsedSeconds());
            }
        }
    }

    private void restart() {
        String elapsedSeconds = String.valueOf(this.elapsedSeconds());
        boolean hasVideo = this.renderer.videoExecutor != null;
        boolean hasAudio = this.renderer.audioExecutor != null;

        if (hasVideo) this.renderer.videoExecutor.shutdown();
        if (hasAudio) this.renderer.audioExecutor.shutdown();

        if (hasVideo) this.renderer.videoExecutor.setup();
        if (hasAudio) this.renderer.audioExecutor.setup();
    }
}