package me.kall.narutoloading.mixin.inworld;

import it.unimi.dsi.fastutil.longs.LongSets;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockOcclusionCache.class, remap = false)
public abstract class MixinBlockOcclusionCache {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void skip(BlockState selfState, BlockGetter view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && ClientScreensRenderer.HIDDEN_DISPLAYERS.getOrDefault(level.dimension().location(), LongSets.emptySet()).contains(pos.asLong())) {
            cir.setReturnValue(false);
        }
    }
}
