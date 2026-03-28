package me.kall.narutoloading.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LifetimeController {
    private final AtomicLong absoluteSetupTime;
    private final AtomicLong pausedAt = new AtomicLong(0L);
    private final AtomicLong lastFetchFrameTime = new AtomicLong(-1L);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    public  final AtomicBoolean lagSpikeDetected = new AtomicBoolean(false);
    private final AtomicLong lastLagSpikeRestart = new AtomicLong(-1L);

    public  final AtomicBoolean syncSoundEngine = new AtomicBoolean(false);

    private final Supplier<Runnable> restarter;
    private final Supplier<Consumer<String>> synchronizer;
    private final double  duration;
    private final BooleanSupplier audioAvailable;

    public LifetimeController(double duration, long absoluteSetupTime, Supplier<Runnable> restarter, Supplier<Consumer<String>> synchronizer, BooleanSupplier audioAvailable) {
        this.duration = duration;
        this.absoluteSetupTime = new AtomicLong(absoluteSetupTime);
        this.restarter = restarter;
        this.synchronizer = synchronizer;
        this.audioAvailable = audioAvailable;
    }

    public void pause() {
        if (!this.paused.get() && this.running.get() && !this.audioAvailable.getAsBoolean()) {
            this.paused.set(true);
            this.pausedAt.set(System.nanoTime());
        }
    }

    public void resume() {
        if (this.paused.get() && this.running.get() && !this.audioAvailable.getAsBoolean()) {
            this.paused.set(false);
            this.absoluteSetupTime.addAndGet(System.nanoTime() - this.pausedAt.get());
        }
    }

    public boolean shouldUpdateFrame(double fps) {
        if (this.paused.get()) return false;
        long now = System.nanoTime();
        long last = this.lastFetchFrameTime.get();

        if (last == -1L) {
            this.lastFetchFrameTime.set(now);
            return true;
        }

        if ((double)(now - last) >= 1_000_000_000.0 / fps) {
            this.lastFetchFrameTime.set(now);
            return true;
        }
        return false;
    }

    public void start() {
        this.running.set(true);
    }

    public void stop() {
        this.running.set(false);
    }

    public boolean isRunning() {
        return this.running.get();
    }

    public double elapsedSeconds() {
        return this.elapsedMillis() / 1000.0D;
    }

    public long elapsedMillis() {
        return (System.nanoTime() - this.absoluteSetupTime.get()) / 1_000_000L;
    }

    public void endRestart() {
        if (this.elapsedMillis() >= this.duration) {
            this.restarter.get().run();
        }
    }

    public void lagSpikeRestart() {
        if (this.lagSpikeDetected.compareAndSet(true, false)) {
            long last = this.lastLagSpikeRestart.get();
            if (last == -1L) {
                this.lastLagSpikeRestart.set(System.nanoTime());
                return;
            }
            if (System.nanoTime() - last > 2_000_000_000L) {
                this.synchronize();
                this.lastLagSpikeRestart.set(System.nanoTime());
            }
        }
    }

    public void syncSoundEngine() {
        if (this.syncSoundEngine.compareAndSet(true, false)) {
            if (this.audioAvailable.getAsBoolean()) {
                this.synchronize();
            }
        }
    }

    private void synchronize() {
        long start = System.nanoTime();
        this.synchronizer.get().accept(String.valueOf(this.elapsedSeconds()));
        this.absoluteSetupTime.addAndGet(System.nanoTime() - start);
    }

    public void seekTo(double seconds) {
        this.absoluteSetupTime.set(System.nanoTime() - (long)(seconds * 1_000_000_000.0));
        this.lastFetchFrameTime.set(-1L);
    }
}