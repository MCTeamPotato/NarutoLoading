package me.kall.narutoloading.common;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LifetimeController {
    private long absoluteSetupTime;
    private long pausedAt = 0L;
    private long lastFetchFrameTime = -1;

    private boolean running = false;
    private boolean paused = false;

    public volatile boolean lagSpikeDetected = false;
    private long lastLagSpikeRestart = -1;

    public volatile boolean syncSoundEngine = false;

    private final Supplier<Runnable> restarter;
    private final Supplier<Consumer<String>> synchronizer;
    private final long duration;
    private final BooleanSupplier audioAvailable;

    public LifetimeController(long duration, long absoluteSetupTime, Supplier<Runnable> restarter, Supplier<Consumer<String>> synchronizer, BooleanSupplier audioAvailable) {
        this.duration = duration;
        this.absoluteSetupTime = absoluteSetupTime;
        this.restarter = restarter;
        this.synchronizer = synchronizer;
        this.audioAvailable = audioAvailable;
    }

    public void pause() {
        if (!this.paused && this.running && !this.audioAvailable.getAsBoolean()) {
            this.paused = true;
            this.pausedAt = System.nanoTime();
        }
    }

    public void resume() {
        if (this.paused && this.running && !this.audioAvailable.getAsBoolean()) {
            this.paused = false;
            this.absoluteSetupTime += System.nanoTime() - this.pausedAt;
        }
    }

    public boolean shouldUpdateFrame(double fps) {
        if (this.paused) return false;
        long now = System.nanoTime();

        if (this.lastFetchFrameTime == -1L) {
            this.lastFetchFrameTime = now;
            return true;
        }

        double intervalNanos = (double) now - (double) this.lastFetchFrameTime;
        if (intervalNanos >= (1_000_000_000.0 / fps)) {
            this.lastFetchFrameTime = now;
            return true;
        } else {
            return false;
        }
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
            this.restarter.get().run();
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
                this.synchronize();
                this.lastLagSpikeRestart = System.nanoTime();
            }
        }
    }

    public void syncSoundEngine() {
        if (this.syncSoundEngine) {
            this.syncSoundEngine = false;
            if (this.audioAvailable.getAsBoolean()) {
                this.synchronize();
            }
        }
    }

    private void synchronize() {
        long restartStartTime = System.nanoTime();
        this.synchronizer.get().accept(String.valueOf(this.elapsedSeconds()));
        this.absoluteSetupTime += (System.nanoTime() - restartStartTime);
    }
}