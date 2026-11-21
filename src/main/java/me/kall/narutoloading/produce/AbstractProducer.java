package me.kall.narutoloading.produce;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public abstract class AbstractProducer {
    private static final File PRODUCER_ERROR_LOG = Path.of(System.getProperty("user.dir", ".")).resolve("logs").resolve("narutoloading-error.log").toFile();
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH.mm.ss");

    static {
        File logDir = PRODUCER_ERROR_LOG.getParentFile();

        if (!logDir.exists()) logDir.mkdirs();

        if (PRODUCER_ERROR_LOG.exists() && PRODUCER_ERROR_LOG.length() > 0) {
            String timeName = LocalDateTime.now().format(LOG_TIME_FORMATTER);
            File gzFile = new File(logDir, "narutoloading-" + timeName + ".log.gz");
            try (FileInputStream fileInputStream = new FileInputStream(PRODUCER_ERROR_LOG); FileOutputStream fileOutputStream = new FileOutputStream(gzFile); GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fileInputStream.read(buffer)) != -1) {
                    gzipOutputStream.write(buffer, 0, len);
                }
            } catch (IOException e) {
                System.err.println("[NarutoLoading] Failed to compress old error log: " + e.getMessage());
            }

            try {
                Files.delete(PRODUCER_ERROR_LOG.toPath());
            } catch (IOException e) {
                System.err.println("[NarutoLoading] Failed to delete old error log: " + e.getMessage());
            }
        }

        try {
            PRODUCER_ERROR_LOG.createNewFile();
        } catch (IOException e) {
            System.err.println("[NarutoLoading] Failed to create new error log: " + e.getMessage());
        }
    }

    public final AtomicBoolean off = new AtomicBoolean(true);

    protected final AtomicBoolean ready = new AtomicBoolean();
    private final AtomicBoolean eager = new AtomicBoolean();
    private final Object waiter = new Object();

    private final AtomicReference<ExecutorService> executor = new AtomicReference<>();
    private final AtomicReference<Process> process = new AtomicReference<>();
    private final AtomicReference<InputStream> input = new AtomicReference<>();
    private final AtomicReference<Runnable> warmListener = new AtomicReference<>();

    protected abstract @Nullable String[] setCommand(double seekTo);

    protected abstract void forInput(InputStream input) throws IOException, InterruptedException;

    public void setup(double seekTo) {
        String[] command = this.setCommand(seekTo);
        if (command == null || command[0] == null) return;

        this.off.set(false);
        this.onSetup(seekTo);
        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoProducer_" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        });

        this.executor.set(executor);
        executor.submit(() -> {
            try {
                Process process = new ProcessBuilder(command).redirectError(PRODUCER_ERROR_LOG).start();
                this.process.set(process);

                InputStream input = process.getInputStream();
                this.input.set(input);

                this.forInput(input);
            } catch (IOException | InterruptedException exception) {
                if (this.off.get()) return;
                System.err.println("[NarutoLoading] Exception during " + Arrays.toString(command) + " execution: " + exception.getMessage());
            }
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
        this.off.set(true);

        synchronized (this.waiter) {
            this.eager.set(false);
            this.ready.set(false);
            this.waiter.notify();
        }

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
}
