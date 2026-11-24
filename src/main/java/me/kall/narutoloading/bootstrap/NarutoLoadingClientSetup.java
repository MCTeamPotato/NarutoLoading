package me.kall.narutoloading.bootstrap;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.client.input.NarutoKeys;
import me.kall.narutoloading.wall.client.WallReloadListener;
import me.kall.narutoloading.wall.client.render.WallShaders;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoLoading.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NarutoLoadingClientSetup {
    private NarutoLoadingClientSetup() {}

    @SubscribeEvent
    public static void registerKeys(@NotNull RegisterKeyMappingsEvent event) {
        NarutoKeys.register(event);
    }

    @SubscribeEvent
    public static void registerShaders(@NotNull RegisterShadersEvent event) {
        WallShaders.register(event);
    }

    @SubscribeEvent
    public static void registerReloadListeners(@NotNull RegisterClientReloadListenersEvent event) {
        WallReloadListener.register(event);
    }
}
