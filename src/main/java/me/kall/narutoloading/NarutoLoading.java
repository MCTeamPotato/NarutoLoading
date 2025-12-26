package me.kall.narutoloading;

import me.kall.narutoloading.core.NarutoInWorldRenderer;
import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.init.NarutoBlocks;
import me.kall.narutoloading.init.NarutoItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";
    public static final Logger LOGGER = LogManager.getLogger(NarutoLoading.class);

    public NarutoLoading(@NotNull FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        NarutoBlocks.BLOCKS.register(modBus);
        NarutoItems.ITEMS.register(modBus);

        if (FMLLoader.getDist().isClient()) {
            forgeBus.addListener(NarutoLoadingClient::onBuildCreativeTab);
            forgeBus.addListener(NarutoRenderer.INSTANCE.windowSizeChecker::clientTick);
            forgeBus.addListener(NarutoRenderer.INSTANCE.keyChecker::clientTick);
            forgeBus.addListener(NarutoInWorldRenderer.INSTANCE::onRenderLevel);
            forgeBus.addListener(NarutoInWorldRenderer.INSTANCE::onRenderTick);

            modBus.addListener(NarutoLoadingClient::onClientSetup);
        }
    }
}
