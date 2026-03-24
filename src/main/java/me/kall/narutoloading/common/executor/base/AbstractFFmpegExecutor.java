package me.kall.narutoloading.common.executor.base;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public abstract class AbstractFFmpegExecutor {
    protected volatile boolean canceled;

    protected @Nullable ExecutorService executor;
    protected @Nullable Process process;
    protected @Nullable InputStream inputStream;

    protected final Supplier<String> ffmpeg;
    protected final BooleanSupplier debug;

    protected AbstractFFmpegExecutor(Supplier<String> ffmpeg, BooleanSupplier debug) {
        this.ffmpeg = ffmpeg;
        this.debug = debug;
    }

    public void setup(String sec) {
        this.canceled = false;

        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, this.getClass().getSimpleName());
            thread.setDaemon(true);
            return thread;
        });

        this.executor.submit(() -> {
            try {
                ProcessBuilder pb = buildProcess(sec);
                this.process = pb.start();
                this.inputStream = this.process.getInputStream();

                runLoop(this.inputStream);
            } catch (Exception e) {
                if (this.debug.getAsBoolean()) onError(e);
            }
        });
    }

    public final void setup() {
        setup("0");
    }

    public void shutdown() {
        this.canceled = true;

        if (this.process != null) {
            this.process.destroyForcibly();
            this.process = null;
        }

        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }

        try {
            if (this.inputStream != null) {
                this.inputStream.close();
                this.inputStream = null;
            }
        } catch (Exception ignored) {}

        cleanup();
    }

    protected void onError(Exception e) {}

    protected abstract ProcessBuilder buildProcess(String sec);

    protected abstract void runLoop(InputStream inputStream) throws Exception;

    protected void cleanup() {}
}