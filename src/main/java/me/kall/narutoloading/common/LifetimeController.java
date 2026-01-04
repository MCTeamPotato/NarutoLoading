package me.kall.narutoloading.common;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.noworld.core.NarutoRenderer;

public class LifetimeController {
    private long lastFrameTime = 0L;
    private long startTime = -1L;
    private long elapsedTime = 0L;
    private long pausedAt = 0L;

    private boolean running = false;
    private boolean paused = false;

    public volatile boolean lagSpikeDetected = false;
    protected long lastLagSpikeRestart = -1;

    public volatile boolean syncSoundEngine = false;

    protected NarutoRenderer renderer;
    private final long duration;

    private double accumulatedFrameTime = 0.0;

    public LifetimeController(NarutoRenderer renderer, long duration) {
        this.renderer = renderer;
        this.duration = duration;
    }

    public void tick() {
        if (this.paused) return;
        long now = System.currentTimeMillis();
        if (this.startTime == -1L) this.startTime = now;
        this.elapsedTime = now - this.startTime;
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
            long pauseDuration = System.currentTimeMillis() - this.pausedAt;
            this.startTime += pauseDuration;
        }
    }

    public boolean shouldUpdateFrame(int fps) {
        if (this.paused) return false;

        long now = System.currentTimeMillis();

        double frameInterval = 1000.0D / (double) fps;
        double elapsed = now - this.lastFrameTime;

        if (elapsed >= frameInterval) {
            this.accumulatedFrameTime += elapsed;
            double framesToAdvance = Math.floor(this.accumulatedFrameTime / frameInterval);
            this.accumulatedFrameTime -= framesToAdvance * frameInterval;
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
            long current = System.currentTimeMillis();
            if (this.lastLagSpikeRestart == -1) {
                this.lastLagSpikeRestart = current;
                return;
            }
            if (current - this.lastLagSpikeRestart < 5000) return;

            long restartStartTime = System.currentTimeMillis();
            double targetSeconds = this.elapsedSeconds();
            String sec = String.valueOf(targetSeconds);

            NarutoLoading.LOGGER.warn("Lag spike detected, restarting video and audio from {} seconds", sec);

            boolean hasVideo = this.renderer.videoExecutor != null;
            boolean hasAudio = this.renderer.audioExecutor != null;

            if (hasVideo) this.renderer.videoExecutor.shutdown();
            if (hasAudio) this.renderer.audioExecutor.shutdown();

            if (hasVideo) this.renderer.videoExecutor.setup(sec);
            if (hasAudio) this.renderer.audioExecutor.setup(sec);

            long restartDuration = System.currentTimeMillis() - restartStartTime;
            this.startTime += restartDuration;

            NarutoLoading.LOGGER.info("{}Restart completed, compensated {} ms", NarutoLoading.info(), restartDuration);

            this.lastLagSpikeRestart = System.currentTimeMillis();
        }
    }

    public void syncSoundEngine() {
        if (this.syncSoundEngine) {
            this.syncSoundEngine = false;
            if (this.renderer.audioExecutor != null) {
                long syncStartTime = System.currentTimeMillis();
                double syncTime = this.elapsedSeconds();

                NarutoLoading.LOGGER.info("{}Syncing audio to {} seconds after sound engine reload", NarutoLoading.info(), syncTime);

                this.renderer.audioExecutor.setup(String.valueOf(syncTime));

                if (this.renderer.videoExecutor != null) {
                    this.renderer.videoExecutor.shutdown();
                    this.renderer.videoExecutor.setup(String.valueOf(syncTime));
                }

                long syncDuration = System.currentTimeMillis() - syncStartTime;
                this.startTime += syncDuration;
            }
        }
    }
}