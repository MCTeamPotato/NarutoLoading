package me.kall.narutoloading.core.executor;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractFFmpegExecutor {
    protected final AtomicBoolean canceled = new AtomicBoolean(false);

    private final AtomicReference<ExecutorService> executor = new AtomicReference<>();
    private final AtomicReference<Process> process = new AtomicReference<>();
    private final AtomicReference<InputStream> inputStream = new AtomicReference<>();

    public final void setup() {
        this.setup("0");
    }

    public void setup(String seconds) {
        this.canceled.set(false);

        ExecutorService newExecutor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, this.getClass().getSimpleName());
            thread.setDaemon(true);
            return thread;
        });
        this.executor.set(newExecutor);

        newExecutor.submit(() -> {
            try {
                Process p = new ProcessBuilder(this.command(seconds)).redirectErrorStream(true).start();
                this.process.set(p);
                InputStream is = p.getInputStream();
                this.inputStream.set(is);
                this.runLoop(is);
            } catch (Exception exception) {
                if (!this.canceled.get()) {
                    throw new RuntimeException(exception);
                }
            }
        });
    }

    public void shutdown() {
        this.canceled.set(true);

        ExecutorService executorService = this.executor.getAndSet(null);
        if (executorService != null) executorService.shutdownNow();

        Process process = this.process.getAndSet(null);
        if (process != null) process.destroyForcibly();

        InputStream inputStream = this.inputStream.getAndSet(null);
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        this.cleanup();
    }

    protected abstract String[] command(String seconds);
    protected abstract void runLoop(@NotNull InputStream inputStream) throws Exception;
    protected abstract void cleanup();
}