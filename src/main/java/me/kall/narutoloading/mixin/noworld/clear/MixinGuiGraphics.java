package me.kall.narutoloading.mixin.noworld.clear;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.textures.GpuTextureView;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.noworld.fade.Fader;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = GuiGraphics.class, priority = 500)
public abstract class MixinGuiGraphics {
    @ModifyVariable(method = "submitColoredRectangle", at = @At(value = "HEAD"), ordinal = 4, argsOnly = true)
    private int modifyColorFrom(int colorFrom) {
        return Fader.modifyAlpha(colorFrom);
    }

    @ModifyVariable(method = "submitColoredRectangle", at = @At(value = "HEAD"), ordinal = 0, argsOnly = true)
    private Integer modifyColorTo(Integer colorFrom) {
        if (colorFrom == null) return null;
        return Fader.modifyAlpha(colorFrom);
    }

    @ModifyVariable(method = "submitBlit", at = @At("HEAD"), ordinal = 4, argsOnly = true)
    private int modifyBlitColor(int color, @Local(argsOnly = true) GpuTextureView atlasTexture) {
        if (atlasTexture.texture().getLabel().equals(NarutoLoading.VIDEO_TEXTURE_LABEL)) return color;
        return Fader.modifyAlpha(color);
    }

    @ModifyVariable(method = "submitTiledBlit", at = @At("HEAD"), ordinal = 6, argsOnly = true)
    private int modifyTiledBlitColor(int color) {
        return Fader.modifyAlpha(color);
    }
}
