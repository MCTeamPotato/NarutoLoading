package me.kall.narutoloading;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";
    public static final Logger LOGGER = LogManager.getLogger(NarutoLoading.class);

    public static final ThreadLocal<GuiGraphics> GUI_GRAPHICS = new ThreadLocal<>();

    public static final Component EMPTY_COMPONENT = Component.empty();
    public static final String BLANK = "";

    public NarutoLoading(@NotNull FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
    }
}
