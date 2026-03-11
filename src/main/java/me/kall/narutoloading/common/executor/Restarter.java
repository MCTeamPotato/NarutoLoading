package me.kall.narutoloading.common.executor;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import me.kall.narutoloading.NarutoLoading;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Map;

@EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public final class Restarter {
    public static final Map<Runnable, Runnable> RESTART_TASKS = new Object2ObjectArrayMap<>();

    private static int interval = 20;

    public static void pend(Runnable shutdown, Runnable setup) {
        synchronized (Restarter.RESTART_TASKS) {
            Restarter.RESTART_TASKS.put(shutdown, setup);
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Pre event) {
        interval--;
        if (interval != 0) return;
        interval = 20;

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
