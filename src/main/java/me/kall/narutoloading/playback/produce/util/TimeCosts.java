package me.kall.narutoloading.playback.produce.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;

public class TimeCosts {
    private static final double NANOS_PER_MILLI = 1_000_000.0D;

    public final String label;

    private final int size;
    private final long[] costs;
    private final long budgetNanos;

    private int index;

    private int count;

    public TimeCosts(String label, int size, double budgetMillis) {
        this.label = label;
        this.size = Math.max(1, size);
        this.costs = new long[this.size];
        this.budgetNanos = budgetMillis > 0D ? (long) (budgetMillis * NANOS_PER_MILLI) : 0L;
    }

    public TimeCosts(String label, int size) {
        this(label, size, 0D);
    }

    public synchronized void record(long ns) {
        this.costs[this.index] = Math.max(ns, 0L);
        this.index = (this.index + 1) % this.size;
        if (this.count < this.size) this.count++;
    }

    public synchronized void reset() {
        this.index = 0;
        this.count = 0;
        Arrays.fill(this.costs, 0L);
    }

    public synchronized boolean isEmpty() {
        return this.count == 0;
    }

    public @NotNull String describe() {
        long[] window = this.sortedWindow();
        int n = window.length;
        if (n == 0) return this.label + " n=0";

        long sum = 0L;
        for (long cost : window) sum += cost;

        StringBuilder out = new StringBuilder(72);
        out.append(this.label).append(" n=").append(n);
        out.append(" avg=").append(millis(sum / (double) n)).append("ms");
        out.append(" p50=").append(millis(window[n / 2])).append("ms");
        out.append(" p95=").append(millis(window[Math.min(n - 1, (int) Math.ceil(n * 0.95D) - 1)])).append("ms");
        out.append(" max=").append(millis(window[n - 1])).append("ms");

        if (this.budgetNanos > 0L) {
            int over = 0;
            for (long cost : window) if (cost > this.budgetNanos) over++;
            out.append(" over=").append(over).append('/').append(n);
        }

        return out.toString();
    }

    private synchronized long @NotNull [] sortedWindow() {
        long[] window = new long[this.count];
        if (this.count == 0) return window;

        int start = (this.index - this.count + this.size) % this.size;
        for (int i = 0; i < this.count; i++) window[i] = this.costs[(start + i) % this.size];
        Arrays.sort(window);
        return window;
    }

    private static @NotNull String millis(double nanos) {
        return String.format(Locale.ROOT, "%.2f", nanos / NANOS_PER_MILLI);
    }
}
