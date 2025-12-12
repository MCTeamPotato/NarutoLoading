package me.kall.narutoloading.mixin.clear;

import me.kall.narutoloading.NarutoLoadingClient;
import me.kall.narutoloading.core.MouseChecker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiGraphics.class, priority = 500)
public abstract class MixinGuiGraphics {
    @ModifyVariable(method = "setColor", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float modifyAlpha(float alpha) {
        return alpha * MouseChecker.fadeAlpha;
    }

    @ModifyVariable(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V", at = @At("HEAD"), argsOnly = true, ordinal = 5)
    private int modifyColor(int color) {
        return MouseChecker.modifyAlpha(color);
    }

    @ModifyVariable(method = "fillGradient(Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIII)V", at = @At("HEAD"), argsOnly = true, ordinal = 5)
    private int modifyColorFrom(int color) {
        return MouseChecker.modifyAlpha(color);
    }

    @ModifyVariable(method = "fillGradient(Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIII)V", at = @At("HEAD"), argsOnly = true, ordinal = 6)
    private int modifyColorTo(int color) {
        return MouseChecker.modifyAlpha(color);
    }

    @ModifyVariable(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 7)
    private float modifyBlitAlpha(float alpha) {
        return alpha * MouseChecker.fadeAlpha;
    }

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At("HEAD"), cancellable = true)
    private void hideTexture(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, CallbackInfo ci) {
        ResourceLocation texture = NarutoLoadingClient.RENDERER.texture();
        if (texture == null) return;
        if (MouseChecker.transparency() && !atlasLocation.equals(texture)) ci.cancel();
    }
}
