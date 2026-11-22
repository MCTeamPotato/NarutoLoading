package me.kall.narutoloading.mixin.cull;

import me.kall.narutoloading.wall.client.cull.SurfaceTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockOcclusionCache;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockOcclusionCache.class, remap = false)
public abstract class MixinBlockOcclusionCache {
    @Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true, require = 0)
    private void hideForVideo(BlockState state, BlockGetter level, BlockPos pos, Direction facing, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && SurfaceTracker.hidesFace(pos, facing)) cir.setReturnValue(false);
    }
}
