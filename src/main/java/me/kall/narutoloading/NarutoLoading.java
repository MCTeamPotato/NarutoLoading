package me.kall.narutoloading;

import me.kall.narutoloading.inworld.init.NarutoBlocks;
import me.kall.narutoloading.inworld.init.NarutoItems;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";
    public static final Logger LOGGER = LogManager.getLogger(NarutoLoading.class);

    public static final String BLANK = "";
    private static final String PREFIX = "[NarutoLoading] ";

    public NarutoLoading(IEventBus modBus, Dist dist, ModContainer container) {
        NarutoBlocks.BLOCKS.register(modBus);
        NarutoItems.ITEMS.register(modBus);
        modBus.addListener(NarutoPackets::register);
    }

    public static String info() {
        return PREFIX;
    }
}
