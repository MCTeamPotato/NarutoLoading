package me.kall.narutoloading.bootstrap;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.network.NarutoNetwork;
import me.kall.narutoloading.wall.server.WallBlockChanges;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NarutoLoadingSetup {
    private NarutoLoadingSetup() {}

    @SubscribeEvent
    public static void setup(@NotNull FMLCommonSetupEvent event) {
        event.enqueueWork(NarutoNetwork::register);

        if (Integration.DUPLICATIONLESS) {
            event.enqueueWork(() -> MinecraftForge.EVENT_BUS.register(new WallBlockChanges()));
        }
    }
}
