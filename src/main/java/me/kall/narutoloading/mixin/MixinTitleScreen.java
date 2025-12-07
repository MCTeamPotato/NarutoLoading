package me.kall.narutoloading.mixin;

import me.kall.narutoloading.NarutoVideoPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"))
    private void disable(PanoramaRenderer instance, float f, float deltaT) {}

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"))
    private void disable(GuiGraphics instance, ResourceLocation atlasLocation, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {}

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableBlend()V", remap = false, shift = At.Shift.AFTER))
    private void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ResourceLocation texture = NarutoVideoPlayer.getCurrentFrameTexture();
        if (texture != null) {
            int w = graphics.guiWidth();
            int h = graphics.guiHeight();;
            graphics.blit(texture, 0, 0, 0, 0, w, h, w, h);
        }
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void remove(CallbackInfo ci) {
        NarutoVideoPlayer.shutdown();
    }
}
