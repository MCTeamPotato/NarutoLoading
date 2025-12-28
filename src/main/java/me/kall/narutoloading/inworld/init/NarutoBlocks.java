package me.kall.narutoloading.inworld.init;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.block.Displayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class NarutoBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, NarutoLoading.MOD_ID);

    public static final RegistryObject<Block> DISPLAYER = BLOCKS.register("displayer", Displayer::new);
}
