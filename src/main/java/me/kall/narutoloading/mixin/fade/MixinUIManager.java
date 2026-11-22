package me.kall.narutoloading.mixin.fade;

import com.mojang.blaze3d.vertex.VertexConsumer;
import icyllis.modernui.mc.UIManager;
import me.kall.narutoloading.client.fade.FadeCenter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("UnstableApiUsage")
@Mixin(UIManager.class)
public abstract class MixinUIManager {
    @Unique
    private VertexConsumer fade$color(VertexConsumer consumer, int red, int green, int blue, int alpha) {
        if (FadeCenter.isFull()) return consumer.color(red, green, blue, alpha);

        float fade = FadeCenter.fadeAlpha();
        return consumer.color(Math.round(red * fade), Math.round(green * fade), Math.round(blue * fade), Math.round(alpha * fade));
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0))
    private VertexConsumer fade$color0(VertexConsumer consumer, int red, int green, int blue, int alpha) {
        return this.fade$color(consumer, red, green, blue, alpha);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 1))
    private VertexConsumer fade$color1(VertexConsumer consumer, int red, int green, int blue, int alpha) {
        return this.fade$color(consumer, red, green, blue, alpha);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 2))
    private VertexConsumer fade$color2(VertexConsumer consumer, int red, int green, int blue, int alpha) {
        return this.fade$color(consumer, red, green, blue, alpha);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 3))
    private VertexConsumer fade$color3(VertexConsumer consumer, int red, int green, int blue, int alpha) {
        return this.fade$color(consumer, red, green, blue, alpha);
    }
}
