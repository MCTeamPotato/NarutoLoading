package me.kall.narutoloading.mixin.light;

import me.kall.narutoloading.world.WallLights;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class MixinBlockLight {
    @Inject(method = "getLightEmission", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void addWallLight(BlockState state, BlockGetter level, BlockPos pos, @NotNull CallbackInfoReturnable<Integer> cir) {
        int extra = WallLights.emissionAt(level, pos);
        if (extra > cir.getReturnValueI()) cir.setReturnValue(extra);
    }
}
