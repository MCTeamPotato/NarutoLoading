package me.kall.narutoloading.mixin.noworld.impl.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.client.ClientModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LoadingOverlay.class, priority = 500)
public abstract class MixinLoadingOverlay {

    @Shadow protected abstract void drawProgressBar(PoseStack guiGraphics, int minX, int minY, int maxX, int maxY, float partialTick);

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bind(Lnet/minecraft/resources/ResourceLocation;)V"))
    private void logoByeBye(TextureManager instance, ResourceLocation resource) {
        if (BaseEnv.available()) return;
        instance.bind(resource);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;drawProgressBar(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIF)V"))
    private void barByeBye(LoadingOverlay instance, PoseStack guiGraphics, int minX, int minY, int maxX, int maxY, float partialTick) {
        if (BaseEnv.available()) return;
        this.drawProgressBar(guiGraphics, minX, minY, maxX, maxY, partialTick);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/fml/client/ClientModLoader;renderProgressText()V"))
    private void progressTextByeBye() {
        if (BaseEnv.available()) return;
        ClientModLoader.renderProgressText();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V"))
    private void fillByeBye(PoseStack poseStack, int minX, int minY, int maxX, int maxY, int color) {
        if (BaseEnv.available()) return;
        GuiComponent.fill(poseStack, minX, minY, maxX, maxY, color);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIFFIIII)V", ordinal = 0))
    private void blitByeBye1(PoseStack matrixStack, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        if (BaseEnv.available()) return;
        GuiComponent.blit(matrixStack, x, y, width, height, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIFFIIII)V", ordinal = 1))
    private void blitByeBye2(PoseStack matrixStack, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        if (BaseEnv.available()) return;
        GuiComponent.blit(matrixStack, x, y, width, height, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void render(PoseStack guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (BaseEnv.available()) NarutoRenderer.INSTANCE.renderFrame(guiGraphics);
    }
}
