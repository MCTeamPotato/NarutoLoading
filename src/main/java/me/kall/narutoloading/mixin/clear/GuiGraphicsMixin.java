package me.kall.narutoloading.mixin.clear;

import me.kall.narutoloading.render.MouseChecker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
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
}
