package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public class BlockOcclusionCache {
    public boolean shouldDrawSide(BlockState state, BlockGetter level, BlockPos pos, Direction facing) {
        return true;
    }
}
