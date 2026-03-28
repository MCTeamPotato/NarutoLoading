package me.kall.narutoloading.core.executor;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RestartExecutor {
    public static final long DELAY_NANOSECONDS = 1_000_000_000L;

    private static final ScheduledExecutorService RESTARTER =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread t = new Thread(task, RestartExecutor.class.getSimpleName());
                t.setDaemon(true);
                return t;
            });

    public static void schedule(Runnable restartTask) {
        RESTARTER.schedule(restartTask, DELAY_NANOSECONDS, TimeUnit.NANOSECONDS);
    }

    public static void schedule(Runnable restartTask, long nanoseconds) {
        RESTARTER.schedule(restartTask, nanoseconds, TimeUnit.NANOSECONDS);
    }

    public static void schedule(Runnable shutdownTask, Runnable setupTask, @Nullable Consumer<Runnable> dispatcher) {
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
        }, DELAY_NANOSECONDS, TimeUnit.NANOSECONDS);
    }
}