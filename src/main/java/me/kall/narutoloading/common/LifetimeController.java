package me.kall.narutoloading.common;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;

public class LifetimeController {
    private long absoluteSetupTime;
    private long pausedAt = 0L;
    private long lastFetchFrameTime = -1;

    private boolean running = false;
    private boolean paused = false;

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

    public void pause() {
        if (!this.paused && this.running && this.renderer.audioExecutor == null) {
            this.paused = true;
            this.pausedAt = System.currentTimeMillis();
        }
    }

    public void resume() {
        if (this.paused && this.running) {
            this.paused = false;
            this.absoluteSetupTime += System.currentTimeMillis() - this.pausedAt;
        }
    }

    public boolean shouldUpdateFrame(int fps) {
        if (this.paused) return false;
        long now = System.currentTimeMillis();

        if (this.lastFetchFrameTime == -1L) {
            this.lastFetchFrameTime = now;
            return true;
        }

        double intervalMillis = (double) now - (double) this.lastFetchFrameTime;
        this.lastFetchFrameTime = now;
        return intervalMillis >= (1000.0 / (double) fps);
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
        return System.currentTimeMillis() - this.absoluteSetupTime;
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
                this.lastLagSpikeRestart = System.currentTimeMillis();
                return;
            }

            if (this.lastLagSpikeRestart - System.currentTimeMillis() > 2000L) {
                NarutoLoading.LOGGER.warn("Lag spike detected, restarting video and audio from {} seconds", this.elapsedSeconds());
                this.restart();
                this.lastLagSpikeRestart = System.currentTimeMillis();
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

        if (hasVideo) this.renderer.videoExecutor.setup(elapsedSeconds);
        if (hasAudio) this.renderer.audioExecutor.setup(elapsedSeconds);
    }
}