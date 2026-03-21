package me.kall.narutoloading.mixin.noworld.impl.screen;

import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.WinScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WinScreen.class)
public abstract class MixinWinScreen {
    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void skipBg(GuiGraphics guiGraphics, CallbackInfo ci) {
       if (BaseEnv.available()) {
           ci.cancel();
           NarutoRenderer.INSTANCE.renderFrame(guiGraphics);
       }
    }
}
