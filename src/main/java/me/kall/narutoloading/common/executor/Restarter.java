package me.kall.narutoloading.common.executor;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import me.kall.narutoloading.NarutoLoading;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public final class Restarter {
    public static final Map<Runnable, Runnable> RESTART_TASKS = new Object2ObjectArrayMap<>();

    private static int interval = 20;

    public static void pend(Runnable shutdown, Runnable setup) {
        synchronized (Restarter.RESTART_TASKS) {
            Restarter.RESTART_TASKS.put(shutdown, setup);
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) {

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
}
