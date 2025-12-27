package me.kall.narutoloading.mixin.impl.overlay;

import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LoadingOverlay.class, priority = 500)
public abstract class MixinLoadingOverlay {

    @Shadow protected abstract void drawProgressBar(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, float partialTick);

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"))
    private void logoByeBye(GuiGraphics instance, ResourceLocation atlasLocation, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        if (NarutoRenderer.INSTANCE.ffmpegProvider.available()) return;
        instance.blit(atlasLocation,x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"))
    private void barByeBye(LoadingOverlay instance, GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, float partialTick) {
        if (NarutoRenderer.INSTANCE.ffmpegProvider.available()) return;
        this.drawProgressBar(guiGraphics, minX, minY, maxX, maxY, partialTick);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"))
    private void bgByeBye(GuiGraphics instance, RenderType renderType, int minX, int minY, int maxX, int maxY, int color) {
        if (NarutoRenderer.INSTANCE.ffmpegProvider.available()) return;
        instance.fill(renderType, minX, minY, maxX, maxY, color);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (NarutoRenderer.INSTANCE.ffmpegProvider.available()) NarutoRenderer.INSTANCE.renderFrame(guiGraphics);
    }
}
