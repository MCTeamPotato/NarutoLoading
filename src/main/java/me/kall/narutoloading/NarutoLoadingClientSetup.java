package me.kall.narutoloading;

import me.kall.narutoloading.client.NarutoKeys;
import me.kall.narutoloading.world.WallReload;
import me.kall.narutoloading.world.WorldShaders;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoLoading.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NarutoLoadingClientSetup {
    private NarutoLoadingClientSetup() {}

    @SubscribeEvent
    public static void registerKeys(@NotNull RegisterKeyMappingsEvent event) {
        NarutoKeys.register(event);
    }

    @SubscribeEvent
    public static void registerShaders(@NotNull RegisterShadersEvent event) {
        WorldShaders.register(event);
    }

    @SubscribeEvent
    public static void registerReloadListeners(@NotNull RegisterClientReloadListenersEvent event) {
        WallReload.register(event);
    }
}
