package me.kall.narutoloading.mixin;

import me.kall.narutoloading.NarutoLoading;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class MixinCreateWorldScreen {
    @Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
    private void renderBg(GuiGraphics guiGraphics, CallbackInfo ci) {
        NarutoLoading.RENDERER.renderFrame(guiGraphics);
        ci.cancel();
    }
}
