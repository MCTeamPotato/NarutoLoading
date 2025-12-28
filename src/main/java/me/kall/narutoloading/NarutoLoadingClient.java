package me.kall.narutoloading;

import me.kall.narutoloading.inworld.init.NarutoBlocks;
import me.kall.narutoloading.inworld.init.NarutoItems;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;

public class NarutoLoadingClient {
    public static void onBuildCreativeTab(@NotNull BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) {
            event.accept(NarutoItems.DISPLAYER);
        }
    }

    @SuppressWarnings("removal")
    public static void onClientSetup(@NotNull FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(NarutoBlocks.DISPLAYER.get(), RenderType.translucent()));
    }
}
