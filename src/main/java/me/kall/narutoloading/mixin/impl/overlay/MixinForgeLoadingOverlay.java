package me.kall.narutoloading.mixin.impl.overlay;

import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.loading.ForgeLoadingOverlay;
import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeLoadingOverlay.class, priority = 500)
public abstract class MixinForgeLoadingOverlay {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/earlydisplay/DisplayWindow;render(I)V", remap = false))
    private void windowByeBye(DisplayWindow instance, int alpha) {}

    @Inject(method = "render", at = @At("HEAD"))
    private void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        NarutoRenderer.INSTANCE.renderFrame(guiGraphics);
    }
}