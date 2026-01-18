package me.kall.narutoloading.mixin.noworld.clear;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import me.kall.narutoloading.noworld.fade.Fader;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiGraphics.class, priority = 500)
public abstract class MixinGuiGraphics {
    @ModifyVariable(method = "fill(IIIII)V", at = @At("HEAD"), ordinal = 4, argsOnly = true)
    private int modifyColor(int color) {
        return Fader.modifyAlpha(color);
    }

    @ModifyVariable(method = "fillGradient", at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private int modifyColorFrom(int color) {
        return Fader.modifyAlpha(color);
    }

    @ModifyVariable(method = "fillGradient", at = @At("HEAD"), argsOnly = true, ordinal = 5)
    private int modifyColorTo(int color) {
        return Fader.modifyAlpha(color);
    }

    @Inject(method = "innerBlit", at = @At("HEAD"), cancellable = true)
    private void hideTexture(RenderPipeline pipeline, Identifier atlasLocation, int x0, int x1, int y0, int y1, float u0, float u1, float v0, float v1, int color, CallbackInfo ci) {
        Identifier texture = NarutoRenderer.INSTANCE.textureLocation;
        if (texture == null) return;
        if (Fader.transparency() && !atlasLocation.equals(texture)) {
            ci.cancel();
        }
    }
}
