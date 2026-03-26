package me.kall.narutoloading.core.executor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractFFmpegExecutor {
    protected volatile boolean canceled;

    protected @Nullable ExecutorService executor;
    protected @Nullable Process process;
    protected @Nullable InputStream inputStream;

    public final void setup() {
        this.setup("0");
    }

    public void setup(String seconds) {
        this.canceled = false;

        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, this.getClass().getSimpleName());
            thread.setDaemon(true);
            return thread;
        });

        this.executor.submit(() -> {
            try {
                this.process = new ProcessBuilder(command(seconds)).redirectErrorStream(true).start();
                this.inputStream = this.process.getInputStream();
                this.runLoop(this.inputStream);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    public void shutdown() {
        this.canceled = true;

        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }

        if (this.process != null) {
            this.process.destroyForcibly();
            this.process = null;
        }

        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
            this.inputStream = null;
        }

        this.cleanup();
    }

    protected abstract String[] command(String seconds);
    protected abstract void runLoop(@NotNull InputStream inputStream) throws Exception;
    protected abstract void cleanup();
}
