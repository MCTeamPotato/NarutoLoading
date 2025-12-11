package me.kall.narutoloading.mixin.core.screen;

import me.kall.narutoloading.NarutoLoading;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public class MixinAbstractSelectionList {
    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderBackground:Z"))
    private boolean dirtScreenByeBye1(AbstractSelectionList<?> instance) {
        return false;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderTopAndBottom:Z"))
    private boolean dirtScreenByeBye2(AbstractSelectionList<?> instance) {
        return false;
    }

    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void renderBg(GuiGraphics guiGraphics, CallbackInfo ci) {
        NarutoLoading.RENDERER.renderFrame(guiGraphics);
    }
}
