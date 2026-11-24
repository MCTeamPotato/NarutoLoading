package me.kall.narutoloading.playback.produce;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractProducer {
    private static final File PRODUCER_ERROR_LOG = ErrorLog.initialize();

    public final AtomicBoolean off = new AtomicBoolean(true);

    protected final AtomicBoolean ready = new AtomicBoolean();
    private final AtomicBoolean eager = new AtomicBoolean();
    private final Object waiter = new Object();

    private final ProcessRunner runner = new ProcessRunner();
    private final AtomicReference<Runnable> warmListener = new AtomicReference<>();

    protected abstract @Nullable String[] setCommand(double seekTo);

    protected abstract void forInput(InputStream input) throws IOException, InterruptedException;

    /**
     * Where the process' stdout is read from. The default is the process pipe; producers whose
     * throughput outgrows it (a 4K frame is 12 MB, and a Windows pipe hands out a few KB per read)
     * override this with a stream backed by a larger transport.
     */
    protected @NotNull InputStream openInput(@NotNull Process process) throws IOException {
        return process.getInputStream();
    }

    public void setup(double seekTo) {
        String[] command = this.setCommand(seekTo);
        if (command == null || command[0] == null) return;

        this.off.set(false);
        this.onSetup(seekTo);

        this.runner.start(command, PRODUCER_ERROR_LOG, this::openInput, this::forInput, throwable -> {
            if (this.off.get()) return;
            System.err.println("[NarutoLoading] Exception during " + Arrays.toString(command) + " execution: " + throwable.getMessage());
        });
    }

    protected void onSetup(double seekTo) {}

    protected AbstractProducer withWarmListener(Runnable warmListener) {
        this.warmListener.set(warmListener);
        return this;
    }

    protected void notifyWarmListener() {
        Runnable warmListener = this.warmListener.get();
        if (warmListener != null) warmListener.run();
    }

    public void eager() {
        synchronized (this.waiter) {
            this.eager.set(true);
            this.waiter.notify();
        }
    }

    protected boolean awaitEager() {
        synchronized (this.waiter) {
            while (!this.eager.get() && !this.off.get()) {
                try {
                    this.waiter.wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return this.eager.get() && !this.off.get();
        }
    }

    public void shutdown() {
        this.shutdown(false);
    }

    public void shutdownForExit() {
        this.shutdown(true);
    }

    private void shutdown(boolean await) {
        this.off.set(true);

        synchronized (this.waiter) {
            this.eager.set(false);
            this.ready.set(false);
            this.waiter.notify();
        }

        if (await) this.runner.stopAndAwait(1500L);
        else this.runner.stop();
    }
}
