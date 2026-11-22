package me.kall.narutoloading.playback.produce;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ProcessRunner {
    private final AtomicReference<ExecutorService> executor = new AtomicReference<>();
    private final AtomicReference<Process> process = new AtomicReference<>();
    private final AtomicReference<InputStream> input = new AtomicReference<>();

    public void start(String @NotNull [] command, @Nullable File errorLog, @NotNull InputSource source, @NotNull InputHandler handler, @NotNull Consumer<Throwable> onFailure) {
        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoProducer_" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        });

        this.executor.set(executor);

        executor.submit(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                if (errorLog != null) builder.redirectError(errorLog);

                Process process = builder.start();
                this.process.set(process);

                InputStream input = source.open(process);
                this.input.set(input);

                handler.handle(input);
            } catch (IOException | InterruptedException exception) {
                onFailure.accept(exception);
            }
        });
    }

    public void stop() {
        Process process = this.process.getAndSet(null);
        if (process != null) process.destroyForcibly();

        ExecutorService executor = this.executor.getAndSet(null);
        if (executor != null) executor.shutdownNow();

        InputStream input = this.input.getAndSet(null);
        if (input != null) {
            try {
                input.close();
            } catch (IOException exception) {
                System.err.println("[NarutoLoading] Failed to close input stream: " + exception.getMessage());
            }
        }
    }

    public interface InputHandler {
        void handle(@NotNull InputStream input) throws IOException, InterruptedException;
    }

    /**
     * Opens the byte stream that carries the process' stdout. Defaults to the process pipe, but a
     * producer can hand out another transport instead (see {@link #openInput(Process)}).
     */
    public interface InputSource {
        @NotNull InputStream open(@NotNull Process process) throws IOException;
    }
}
