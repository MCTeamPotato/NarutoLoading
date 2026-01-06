package me.kall.narutoloading;

import me.kall.narutoloading.inworld.init.NarutoItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.jetbrains.annotations.NotNull;

public class NarutoLoadingClient {
    public static void onBuildCreativeTab(@NotNull BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) {
            event.accept(NarutoItems.DISPLAYER.get());
        }
    }
}
