package me.kall.narutoloading.inworld.init;

import me.kall.narutoloading.NarutoLoading;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = NarutoLoading.MOD_ID)
public class NarutoItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NarutoLoading.MOD_ID);

    public static final RegistryObject<Item> DISPLAYER = ITEMS.register("displayer", () -> new BlockItem(NarutoBlocks.DISPLAYER.get(), new Item.Properties().fireResistant()));

    @SubscribeEvent
    public static void onBuildCreativeTab(@NotNull BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) {
            event.accept(NarutoItems.DISPLAYER);
        }
    }
}
