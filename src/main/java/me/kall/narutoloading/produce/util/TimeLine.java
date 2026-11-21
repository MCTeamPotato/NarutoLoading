package me.kall.narutoloading.produce.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;

public class TimeLine {
    public static final double NANO_TO_SEC = 1_000_000_000.0D;

    private final AtomicLong setupTime = new AtomicLong();
    private final AtomicLong seekNanos = new AtomicLong();
    private final AtomicBoolean started = new AtomicBoolean();
    private final long duration;
    private final double fps;

    private final AtomicLong pausedAt = new AtomicLong();
    private final AtomicBoolean paused = new AtomicBoolean();

    private final AtomicLong lastUpdate = new AtomicLong(-1);
    private final AtomicLong lastLagSpike = new AtomicLong(-1);
    private final AtomicBoolean lagSpike = new AtomicBoolean();

    private @Nullable Runnable pauseListener;
    private @Nullable DoubleConsumer resumeListener;

    private TimeLine(double duration, double fps) {
        this.duration = (long) (duration * NANO_TO_SEC);
        this.fps = fps;
    }

    @Contract("_, _ -> new")
    public static @NotNull TimeLine video(double duration, double fps) {
        return new TimeLine(duration, fps);
    }

    @Contract("_ -> new")
    public static @NotNull TimeLine audio(double duration) {
        return new TimeLine(duration, Double.POSITIVE_INFINITY);
    }

    public TimeLine withPauseListener(@NotNull Runnable pauseListener) {
        this.pauseListener = pauseListener;
        return this;
    }

    public TimeLine withResumeListener(@NotNull DoubleConsumer resumeListener) {
        this.resumeListener = resumeListener;
        return this;
    }

    public TimeLine seekTo(double at) {
        long nanos = (long) (at * NANO_TO_SEC);
        this.seekNanos.set(nanos);
        if (this.started.get()) this.setupTime.set(System.nanoTime() - nanos);
        this.lastUpdate.set(-1L);
        return this;
    }

    public void start() {
        if (!this.started.compareAndSet(false, true)) return;
        this.setupTime.set(System.nanoTime() - this.seekNanos.get());
        this.lastUpdate.set(-1L);
    }

    public boolean isStarted() {
        return this.started.get();
    }

    public void detectLagSpike() {
        this.lagSpike.set(true);
    }

    public boolean tick() {
        if (!this.started.get() || this.paused.get()) return false;
        if (!this.lagSpike.compareAndSet(true, false)) return false;

        long last = this.lastLagSpike.get();
        long now = System.nanoTime();

        if (last == -1L) {
            this.lastLagSpike.set(now);
            return false;
        }

        if (now - last <= NANO_TO_SEC * 2D) return false;

        this.lastLagSpike.set(now);
        return true;
    }

    public boolean due() {
        if (this.paused.get()) return false;

        long now = System.nanoTime();
        long last = this.lastUpdate.get();
        if (last == -1L || ((double) now - last) * this.fps >= NANO_TO_SEC) {
            this.lastUpdate.set(now);
            return true;
        }

        return false;
    }

    public boolean paused() {
        return this.paused.get();
    }

    public void pause() {
        if (!this.paused.compareAndSet(false, true)) return;

        this.pausedAt.set(System.nanoTime());
        if (this.pauseListener != null) this.pauseListener.run();
    }

    public void resume() {
        if (!this.paused.compareAndSet(true, false)) return;

        this.setupTime.addAndGet(System.nanoTime() - this.pausedAt.get());
        if (this.resumeListener != null) this.resumeListener.accept(this.positionSec());
    }

    public double positionSec() {
        return (double) this.elapsed() / NANO_TO_SEC;
    }

    public boolean nearEnd() {
        return (double) (this.duration - this.elapsed()) / NANO_TO_SEC <= 2.0D;
    }

    public boolean expired() {
        return this.started.get() && this.elapsed() >= this.duration;
    }

    private long elapsed() {
        long now = System.nanoTime();
        long origin = this.started.get() ? this.setupTime.get() : now - this.seekNanos.get();
        return now - origin;
    }
}
