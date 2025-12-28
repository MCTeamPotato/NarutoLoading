package me.kall.narutoloading.mixin.noworld.impl.screen;

import me.kall.narutoloading.noworld.core.NarutoRenderer;
import me.kall.narutoloading.common.env.BaseEnv;
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
        if (BaseEnv.available()) {
            NarutoRenderer.INSTANCE.renderFrame(guiGraphics);
            ci.cancel();
        }
    }
}
