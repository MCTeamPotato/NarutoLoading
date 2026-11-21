package me.kall.narutoloading.mixin.fade;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = GuiGraphics.class, priority = 500)
public abstract class MixinGuiGraphics {
    @WrapMethod(method = "setColor")
    private void fade$setColor(float red, float green, float blue, float alpha, @NotNull Operation<Void> original) {
        original.call(red, green, blue, alpha * FadeCenter.fadeAlpha());
    }

    @WrapMethod(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V")
    private void fade$fill(RenderType renderType, int minX, int minY, int maxX, int maxY, int z, int color, @NotNull Operation<Void> original) {
        if (FadeCenter.isHidden()) return;
        original.call(renderType, minX, minY, maxX, maxY, z, FadeCenter.modifyAlpha(color));
    }

    @WrapMethod(method = "fillGradient(Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIII)V")
    private void fade$fillGradient(VertexConsumer consumer, int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo, @NotNull Operation<Void> original) {
        if (FadeCenter.isHidden()) return;
        original.call(consumer, x1, y1, x2, y2, z, FadeCenter.modifyAlpha(colorFrom), FadeCenter.modifyAlpha(colorTo));
    }

    @WrapMethod(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V")
    private void fade$innerBlit(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, @NotNull Operation<Void> original) {
        if (!Fadable.isUnfadable(atlasLocation) && FadeCenter.isHidden()) return;
        original.call(atlasLocation, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV);
    }

    @WrapMethod(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V")
    private void fade$innerBlitColored(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, float red, float green, float blue, float alpha, @NotNull Operation<Void> original) {
        boolean unfadable = Fadable.isUnfadable(atlasLocation);
        if (!unfadable && FadeCenter.isHidden()) return;
        original.call(atlasLocation, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV, red, green, blue, unfadable ? alpha : alpha * FadeCenter.fadeAlpha());
    }

    @WrapMethod(method = "renderTooltipInternal")
    private void fade$renderTooltip(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY, ClientTooltipPositioner tooltipPositioner, Operation<Void> original) {
        if (FadeCenter.isHidden()) return;
        original.call(font, components, mouseX, mouseY, tooltipPositioner);
    }

    @WrapMethod(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V")
    private void fade$renderItemDecorations(Font font, ItemStack stack, int x, int y, String text, Operation<Void> original) {
        if (FadeCenter.isHidden()) return;
        original.call(font, stack, x, y, text);
    }
}
