package me.kall.narutoloading.mixin.noworld.clear;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutoloading.noworld.core.checker.FadeChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiComponent.class)
public abstract class MixinGuiComponent {
    @Inject(method = {"fill", "fillGradient(Lcom/mojang/math/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIII)V"}, at = @At("HEAD"), cancellable = true)
    private static void onFill(CallbackInfo ci) {
        if (FadeChecker.transparency()) ci.cancel();
    }

    @Inject(method = "innerBlit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIIIIFFII)V", at = @At("HEAD"), cancellable = true)
    private static void onBlit(PoseStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, CallbackInfo ci) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (textureWidth == w && textureHeight == h) return;
        if (FadeChecker.transparency()) ci.cancel();
    }
}
