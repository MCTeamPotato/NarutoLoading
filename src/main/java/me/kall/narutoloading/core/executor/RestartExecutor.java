package me.kall.narutoloading.core.executor;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RestartExecutor {
    public static final long DELAY_NANOSECONDS = 1_000_000_000L;

    private static final ScheduledExecutorService RESTARTER = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread restartThread = new Thread(task, RestartExecutor.class.getSimpleName());
        restartThread.setDaemon(true);
        return restartThread;
    });

    public static void schedule(Runnable shutdownTask, Runnable setupTask) {
        schedule(shutdownTask, setupTask, DELAY_NANOSECONDS, null);
    }

    public static void schedule(Runnable shutdownTask, Runnable setupTask, long delayNanoseconds, @Nullable Consumer<Runnable> dispatcher) {
        Runnable restartTask = () -> {
            try {
                shutdownTask.run();
                setupTask.run();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        };
        RESTARTER.schedule(() -> {
            if (dispatcher != null) {
                dispatcher.accept(restartTask);
            } else {
                restartTask.run();
            }
        }, delayNanoseconds, TimeUnit.NANOSECONDS);
    }
}