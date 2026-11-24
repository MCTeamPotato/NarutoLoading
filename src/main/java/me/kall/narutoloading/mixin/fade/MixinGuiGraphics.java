package me.kall.narutoloading.mixin.fade;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.client.fade.FadeCenter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiGraphics.class, priority = 1500)
public abstract class MixinGuiGraphics {
    @Shadow
    abstract void innerBlit(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, float red, float green, float blue, float alpha);

    @WrapMethod(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V")
    private void fade$fill(RenderType renderType, int minX, int minY, int maxX, int maxY, int z, int color, @NotNull Operation<Void> original) {
        if (FadeCenter.isHidden()) return;
        original.call(renderType, minX, minY, maxX, maxY, z, FadeCenter.modifyAlpha(color));
    }

    @WrapMethod(method = "fillGradient(Lnet/minecraft/client/renderer/RenderType;IIIIIII)V")
    private void fade$fillGradient(RenderType renderType, int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo, @NotNull Operation<Void> original) {
        if (FadeCenter.isHidden()) return;
        original.call(renderType, x1, y1, x2, y2, z, FadeCenter.modifyAlpha(colorFrom), FadeCenter.modifyAlpha(colorTo));
    }

    @WrapMethod(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V")
    private void fade$innerBlit(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, @NotNull Operation<Void> original) {
        if (FadeCenter.isFull()) {
            original.call(atlasLocation, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV);
            return;
        }

        if (FadeCenter.isHidden()) return;

        this.innerBlit(atlasLocation, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    @WrapMethod(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V")
    private void fade$innerBlitColored(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, float red, float green, float blue, float alpha, @NotNull Operation<Void> original) {
        if (FadeCenter.isHidden()) return;
        original.call(atlasLocation, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV, red, green, blue, alpha * FadeCenter.fadeAlpha());
    }
}
