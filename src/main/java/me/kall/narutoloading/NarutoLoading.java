package me.kall.narutoloading;

import me.kall.narutoloading.inworld.init.NarutoBlocks;
import me.kall.narutoloading.inworld.init.NarutoItems;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";
    public static final Logger LOGGER = LogManager.getLogger(NarutoLoading.class);

    public static final String BLANK = "";
    private static final String PREFIX = "[NarutoLoading] ";

    public NarutoLoading(IEventBus modBus, Dist dist, ModContainer container) {
        IEventBus forgeBus = NeoForge.EVENT_BUS;
        NarutoBlocks.BLOCKS.register(modBus);
        NarutoItems.ITEMS.register(modBus);

        modBus.addListener(NarutoPackets::register);

        if (FMLLoader.getDist().isClient()) {
            //noinspection DataFlowIssue
            forgeBus.addListener(NarutoRenderer.INSTANCE.windowSizeChecker::clientTick);
            //noinspection DataFlowIssue
            forgeBus.addListener(NarutoRenderer.INSTANCE.keyChecker::clientTick);
        }
    }

    public static String info() {
        return PREFIX;
    }
}
