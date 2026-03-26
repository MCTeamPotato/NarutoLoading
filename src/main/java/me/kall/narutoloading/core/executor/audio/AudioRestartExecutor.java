package me.kall.narutoloading.core.executor.audio;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AudioRestartExecutor {
    public static final long DELAY_NANOSECONDS = 1_000_000_000L;

    private static final ScheduledExecutorService RESTARTER = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread restartThread = new Thread(task, AudioRestartExecutor.class.getSimpleName());
        restartThread.setDaemon(true);
        return restartThread;
    });

    public static @NotNull ScheduledFuture<?> schedule(Runnable shutdownTask, Runnable setupTask) {
        return schedule(shutdownTask, setupTask, DELAY_NANOSECONDS, null);
    }

    public static @NotNull ScheduledFuture<?> schedule(Runnable shutdownTask, Runnable setupTask, @Nullable Consumer<Runnable> dispatcher) {
        return schedule(shutdownTask, setupTask, DELAY_NANOSECONDS, dispatcher);
    }

    public static @NotNull ScheduledFuture<?> schedule(Runnable shutdownTask, Runnable setupTask, long delayNanoseconds, @Nullable Consumer<Runnable> dispatcher) {
        Runnable restartTask = () -> {
            try {
                shutdownTask.run();
                setupTask.run();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        };
        return RESTARTER.schedule(() -> {
            if (dispatcher != null) {
                dispatcher.accept(restartTask);
            } else {
                restartTask.run();
            }
        }, delayNanoseconds, TimeUnit.NANOSECONDS);
    }
}