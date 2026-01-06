package me.kall.narutoloading.mixin.noworld.impl.overlay;

import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NeoForgeLoadingOverlay.class, priority = 500)
public abstract class MixinForgeLoadingOverlay {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/earlydisplay/DisplayWindow;render(I)V", remap = false))
    private void windowByeBye(DisplayWindow instance, int alpha) {
        if (BaseEnv.available()) return;
        instance.render(alpha);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (BaseEnv.available()) NarutoRenderer.INSTANCE.renderFrame(guiGraphics);
    }
}