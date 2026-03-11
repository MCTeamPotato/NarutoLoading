package me.kall.narutoloading;

import me.kall.narutoloading.inworld.init.NarutoBlocks;
import me.kall.narutoloading.inworld.init.NarutoItems;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";
    public static final Logger LOGGER = LogManager.getLogger(NarutoLoading.class);

    public static final String BLANK = "";
    private static final String PREFIX = "[NarutoLoading] ";

    public NarutoLoading(@NotNull FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        NarutoBlocks.BLOCKS.register(modBus);
        NarutoItems.ITEMS.register(modBus);
        NarutoPackets.register();
    }

    public static String info() {
        return PREFIX;
    }
}
