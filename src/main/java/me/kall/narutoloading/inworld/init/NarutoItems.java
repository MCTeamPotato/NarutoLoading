package me.kall.narutoloading.inworld.init;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.block.Displayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = NarutoLoading.MOD_ID)
public class NarutoItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, NarutoLoading.MOD_ID);

    public static final DeferredHolder<Item, @NotNull Item> DISPLAYER = ITEMS.register("displayer", () -> new BlockItem(NarutoBlocks.DISPLAYER.get(), new Item.Properties().fireResistant().setId(ResourceKey.create(Registries.ITEM, Displayer.ID))));

    @SubscribeEvent
    public static void onBuildCreativeTab(@NotNull BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) {
            event.accept(NarutoItems.DISPLAYER.get());
        }
    }
}
