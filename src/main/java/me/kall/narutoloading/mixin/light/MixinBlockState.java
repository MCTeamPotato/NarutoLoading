package me.kall.narutoloading.mixin.light;

import me.kall.narutoloading.wall.client.light.WallLights;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.extensions.IForgeBlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public abstract class MixinBlockState implements IForgeBlockState {
    @Override
    public int getLightEmission(BlockGetter level, BlockPos pos) {
        BlockState state = (BlockState) (Object) this;
        return Math.max(WallLights.emissionAt(level, pos), state.getBlock().getLightEmission(state, level, pos));
    }
}
