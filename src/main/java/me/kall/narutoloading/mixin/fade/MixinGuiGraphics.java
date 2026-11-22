package me.kall.narutoloading.mixin.fade;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.narutoloading.client.fade.Fadable;
import me.kall.narutoloading.client.fade.FadeCenter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = GuiGraphics.class, priority = 1500)
public abstract class MixinGuiGraphics {
    @Shadow
    public abstract void innerBlit(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, float red, float green, float blue, float alpha);

    @Unique private boolean fade$unfadable;

    @Inject(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V", at = @At("HEAD"), cancellable = true)
    private void fade$hideFill(RenderType renderType, int minX, int minY, int maxX, int maxY, int z, int color, CallbackInfo ci) {
        if (FadeCenter.isHidden()) ci.cancel();
    }

    @ModifyVariable(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V", at = @At("HEAD"), argsOnly = true, index = 7)
    private int fade$fillColor(int color) {
        return FadeCenter.modifyAlpha(color);
    }

    @Inject(method = "fillGradient(Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIII)V", at = @At("HEAD"), cancellable = true)
    private void fade$hideFillGradient(VertexConsumer consumer, int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo, CallbackInfo ci) {
        if (FadeCenter.isHidden()) ci.cancel();
    }

    @ModifyVariable(method = "fillGradient(Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIII)V", at = @At("HEAD"), argsOnly = true, index = 7)
    private int fade$gradientFrom(int color) {
        return FadeCenter.modifyAlpha(color);
    }

    @ModifyVariable(method = "fillGradient(Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIII)V", at = @At("HEAD"), argsOnly = true, index = 8)
    private int fade$gradientTo(int color) {
        return FadeCenter.modifyAlpha(color);
    }

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At("HEAD"), cancellable = true)
    private void fade$innerBlit(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, CallbackInfo ci) {
        if (Fadable.isUnfadable(atlasLocation) || FadeCenter.isFull()) return;

        ci.cancel();
        if (FadeCenter.isHidden()) return;

        this.innerBlit(atlasLocation, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    @ModifyVariable(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At("HEAD"), argsOnly = true, index = 1)
    private ResourceLocation fade$rememberAtlas(ResourceLocation atlasLocation) {
        this.fade$unfadable = Fadable.isUnfadable(atlasLocation);
        return atlasLocation;
    }

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void fade$hideColored(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!Fadable.isUnfadable(atlasLocation) && FadeCenter.isHidden()) ci.cancel();
    }

    @ModifyVariable(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0), argsOnly = true, index = 14)
    private float fade$alpha(float alpha) {
        return this.fade$unfadable ? alpha : alpha * FadeCenter.fadeAlpha();
    }

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void fade$hideTooltip(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY, ClientTooltipPositioner positioner, CallbackInfo ci) {
        if (FadeCenter.isHidden()) ci.cancel();
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void fade$hideItemDecorations(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (FadeCenter.isHidden()) ci.cancel();
    }
}
