package me.kall.narutoloading.core;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Workers {
    private final ExecutorService probe;
    private final ExecutorService download;

    public Workers() {
        this.probe = single("NarutoProbe");
        this.download = single("NarutoDownload");
    }

    public ExecutorService probe() {
        return this.probe;
    }

    public ExecutorService download() {
        return this.download;
    }

    @Contract("_ -> new")
    private static @NotNull ExecutorService single(String name) {
        return Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, name);
            thread.setDaemon(true);
            return thread;
        });
    }
}
