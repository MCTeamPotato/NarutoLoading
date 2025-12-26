package me.kall.narutoloading.init;

import me.kall.narutoloading.NarutoLoading;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class NarutoItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NarutoLoading.MOD_ID);

    public static final RegistryObject<Item> DISPLAYER = ITEMS.register("displayer", () -> new BlockItem(NarutoBlocks.DISPLAYER.get(), new Item.Properties().fireResistant()));
}
