package me.kall.narutoloading.mixin.inworld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.narutoloading.inworld.data.HiddenDisplayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Function;

@Mixin(BlockRenderDispatcher.class)
public abstract class MixinBlockRenderDispatcher {
    @Inject(method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/function/Function;ZLjava/util/List;)V", remap = false, at = @At("HEAD"), cancellable = true)
    private void skip(BlockState arg, BlockPos pos, BlockAndTintGetter arg3, PoseStack arg4, Function<ChunkSectionLayer, VertexConsumer> bufferLookup, boolean bl, List<BlockModelPart> list, CallbackInfo ci) {
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;
        if (HiddenDisplayers.isHidden(clientLevel.dimension().identifier(), pos)) {
            ci.cancel();
        }
    }
}
