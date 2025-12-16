package me.kall.narutoloading.mixin.impl.screen;

import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.data.FFmpeg;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public class MixinAbstractSelectionList {
    @Shadow private boolean renderTopAndBottom;

    @Shadow private boolean renderBackground;

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderBackground:Z"))
    private boolean dirtScreenByeBye1(AbstractSelectionList<?> instance) {
        if (FFmpeg.available()) return false;
        return this.renderBackground;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderTopAndBottom:Z"))
    private boolean dirtScreenByeBye2(AbstractSelectionList<?> instance) {
        if (FFmpeg.available()) return false;
        return this.renderTopAndBottom;
    }

    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void renderBg(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (FFmpeg.available()) NarutoRenderer.INSTANCE.renderFrame(guiGraphics);
    }
}
