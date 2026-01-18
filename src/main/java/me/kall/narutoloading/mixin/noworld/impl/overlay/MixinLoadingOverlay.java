package me.kall.narutoloading.mixin.noworld.impl.overlay;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LoadingOverlay.class, priority = 500)
public abstract class MixinLoadingOverlay {

    @Shadow protected abstract void drawProgressBar(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, float partialTick);

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"))
    private void logoByeBye(GuiGraphics instance, RenderPipeline pipeline, Identifier atlas, int x, int y, float u, float v, int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight, int color) {
        if (BaseEnv.available()) return;
        instance.blit(pipeline, atlas, x, y, u, v, width, height, uWidth, vHeight, textureWidth, textureHeight, color);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"))
    private void barByeBye(LoadingOverlay instance, GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, float partialTick) {
        if (BaseEnv.available()) return;
        this.drawProgressBar(guiGraphics, minX, minY, maxX, maxY, partialTick);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void bgByeBye(GuiGraphics instance, int minX, int minY, int maxX, int maxY, int color) {
        if (BaseEnv.available()) return;
        instance.fill(minX, minY, maxX, maxY, color);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (BaseEnv.available()) NarutoRenderer.INSTANCE.renderFrame(guiGraphics);
    }
}
