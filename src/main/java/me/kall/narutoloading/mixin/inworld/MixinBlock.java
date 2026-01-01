package me.kall.narutoloading.mixin.inworld;

import it.unimi.dsi.fastutil.longs.LongSets;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.init.NarutoBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class MixinBlock {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void skip(BlockState state, BlockGetter level, BlockPos offset, Direction face, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!state.is(NarutoBlocks.DISPLAYER.get())) return;
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;
        if (ClientScreensRenderer.HIDDEN_DISPLAYERS.getOrDefault(clientLevel.dimension().location(), LongSets.emptySet()).contains(pos.asLong())) {
            cir.setReturnValue(false);
        }
    }
}
