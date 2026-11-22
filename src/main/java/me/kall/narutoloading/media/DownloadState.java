package me.kall.narutoloading.media;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DownloadState {
    private static final int LOG_LINES = 5;

    private final AtomicBoolean downloading = new AtomicBoolean();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private final Object logLock = new Object();
    private final Deque<String> log = new ArrayDeque<>(LOG_LINES);

    private volatile int percent = -1;
    private volatile String status = "";
    private volatile boolean statusFailed;
    private volatile String logText = "";

    public boolean begin() {
        if (!this.downloading.compareAndSet(false, true)) return false;

        this.percent = -1;
        synchronized (this.logLock) {
            this.log.clear();
            this.logText = "";
        }

        this.notifyListeners();
        return true;
    }

    public void end() {
        this.downloading.set(false);
        this.notifyListeners();
    }

    public void status(@NotNull String text, boolean failed) {
        this.status = text;
        this.statusFailed = failed;
        this.notifyListeners();
    }

    public void done(@NotNull String text) {
        this.percent = 100;
        this.status = text;
        this.statusFailed = false;
        this.notifyListeners();
    }

    public void line(@NotNull String line) {
        int parsed = parsePercent(line);
        if (parsed >= 0) this.percent = parsed;

        synchronized (this.logLock) {
            this.log.addLast(line);
            while (this.log.size() > LOG_LINES) this.log.removeFirst();
            this.logText = String.join("\n", this.log);
        }

        this.notifyListeners();
    }

    public boolean downloading() {
        return this.downloading.get();
    }

    public int percent() {
        return this.percent;
    }

    public @NotNull String status() {
        return this.status;
    }

    public boolean statusFailed() {
        return this.statusFailed;
    }

    public @NotNull String logText() {
        return this.logText;
    }

    public void addListener(@NotNull Runnable listener) {
        this.listeners.add(listener);
    }

    public void removeListener(@NotNull Runnable listener) {
        this.listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : this.listeners) listener.run();
    }

    private static int parsePercent(@NotNull String line) {
        if (!line.startsWith("[download]")) return -1;

        int percentIndex = line.indexOf('%');
        if (percentIndex <= 0) return -1;

        int start = percentIndex - 1;
        while (start >= 0 && (Character.isDigit(line.charAt(start)) || line.charAt(start) == '.')) start--;

        try {
            return (int) Math.round(Double.parseDouble(line.substring(start + 1, percentIndex)));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
