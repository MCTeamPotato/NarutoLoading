package me.kall.narutoloading.inworld.init;

import me.kall.narutoloading.NarutoLoading;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NarutoItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, NarutoLoading.MOD_ID);

    public static final DeferredHolder<Item, Item> DISPLAYER = ITEMS.register("displayer", () -> new BlockItem(NarutoBlocks.DISPLAYER.get(), new Item.Properties().fireResistant()));
}
