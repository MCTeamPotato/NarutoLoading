package me.kall.narutoloading.core.base;

import java.util.concurrent.atomic.AtomicLong;

public class VideoArgContainer {
    private final AtomicLong fpsBits = new AtomicLong(Double.doubleToRawLongBits(0.0));
    private final AtomicLong durationBits = new AtomicLong(Double.doubleToRawLongBits(0.0));

    public double getFps() {
        return Double.longBitsToDouble(this.fpsBits.get());
    }

    public double getDuration() {
        return Double.longBitsToDouble(this.durationBits.get());
    }

    public void setFps(double fps) {
        this.fpsBits.set(Double.doubleToRawLongBits(fps));
    }

    public void setDuration(double duration) {
        this.durationBits.set(Double.doubleToRawLongBits(duration));
    }
}
