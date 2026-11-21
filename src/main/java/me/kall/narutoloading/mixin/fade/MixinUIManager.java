package me.kall.narutoloading.mixin.fade;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import icyllis.modernui.mc.UIManager;
import me.kall.narutoloading.client.fade.FadeCenter;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@SuppressWarnings("UnstableApiUsage")
@Mixin(UIManager.class)
public abstract class MixinUIManager {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer fade$color(VertexConsumer consumer, int red, int green, int blue, int alpha, @NotNull Operation<VertexConsumer> original) {
        if (FadeCenter.isFull()) return original.call(consumer, red, green, blue, alpha);

        float fade = FadeCenter.fadeAlpha();
        return original.call(consumer, Math.round(red * fade), Math.round(green * fade), Math.round(blue * fade), Math.round(alpha * fade));
    }
}
