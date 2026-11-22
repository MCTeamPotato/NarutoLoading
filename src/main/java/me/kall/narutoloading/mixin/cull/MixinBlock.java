package me.kall.narutoloading.mixin.cull;

import me.kall.narutoloading.wall.client.cull.SurfaceTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class MixinBlock {
    @Inject(method = "shouldRenderFace", at = @At("RETURN"), cancellable = true, require = 0)
    private static void hideForVideo(BlockState state, BlockGetter level, BlockPos offset, Direction face, BlockPos pos, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && SurfaceTracker.hidesFace(offset, face)) cir.setReturnValue(false);
    }
}
