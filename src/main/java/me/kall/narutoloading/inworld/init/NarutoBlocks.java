package me.kall.narutoloading.inworld.init;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.block.Displayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NarutoBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, NarutoLoading.MOD_ID);

    public static final DeferredHolder<Block, Block> DISPLAYER = BLOCKS.register("displayer", Displayer::new);
}
