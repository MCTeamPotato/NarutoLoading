package me.kall.narutoloading.playback.produce;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ProcessRunner {
    private final AtomicReference<ExecutorService> executor = new AtomicReference<>();
    private final AtomicReference<Process> process = new AtomicReference<>();
    private final AtomicReference<InputStream> input = new AtomicReference<>();

    public void start(String @NotNull [] command, @Nullable File errorLog, @NotNull InputHandler handler, @NotNull Consumer<Throwable> onFailure) {
        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoProducer_" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        });

        this.executor.set(executor);

        executor.submit(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                if (errorLog != null) builder.redirectError(ProcessBuilder.Redirect.appendTo(errorLog));

                Process process = builder.start();
                this.process.set(process);

                InputStream input = process.getInputStream();
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

    /**
     * Stops the worker and waits for it to finish its native cleanup. Used at game shutdown so the
     * audio thread closes its OpenAL device before the process exits.
     */
    public void stopAndAwait(long millis) {
        ExecutorService executor = this.executor.get();
        this.stop();

        if (executor == null) return;

        try {
            executor.awaitTermination(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public interface InputHandler {
        void handle(@NotNull InputStream input) throws IOException, InterruptedException;
    }
}
