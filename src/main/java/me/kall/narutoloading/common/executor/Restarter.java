package me.kall.narutoloading.common.executor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Restarter {
    public static final Map<Runnable, Runnable> RESTART_TASKS = new HashMap<>();

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NarutoLoading-Restarter");
        t.setDaemon(true);
        return t;
    });

    private static final long INTERVAL_NS = 1_000_000_000L;
    private static volatile boolean started = false;

    public static void pend(Runnable shutdown, Runnable setup) {
        ensureStarted();
        synchronized (RESTART_TASKS) {
            RESTART_TASKS.put(shutdown, setup);
        }
    }

    private static void ensureStarted() {
        if (started) return;
        started = true;

        EXECUTOR.execute(() -> {
            long lastTime = System.nanoTime();

            while (!Thread.currentThread().isInterrupted()) {
                long now = System.nanoTime();
                if (now - lastTime >= INTERVAL_NS) {
                    lastTime = now;

                    synchronized (RESTART_TASKS) {
                        if (!RESTART_TASKS.isEmpty()) {
                            RESTART_TASKS.forEach((shutdown, setup) -> {
                                shutdown.run();
                                setup.run();
                            });
                            RESTART_TASKS.clear();
                        }
                    }
                }
            }
        });
    }
}