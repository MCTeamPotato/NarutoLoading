package me.kall.narutoloading.mixin.substitution;

import me.kall.narutoloading.client.overlay.Substitution;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.WinScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WinScreen.class)
public abstract class MixinWinScreen {
    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void substitute(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (Substitution.checkAndRun()) ci.cancel();
    }
}
