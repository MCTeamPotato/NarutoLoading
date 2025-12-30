package me.kall.narutoloading.inworld.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class Displayer extends Block {
    public Displayer() {
        super(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion());
    }
}
