package me.kall.narutoloading.mixin.noworld.impl.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"))
    private void panoramaByeBye(PanoramaRenderer instance, float f, float deltaT) {
        if (BaseEnv.available()) return;
        instance.render(f, deltaT);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/texture/TextureManager;bind(Lnet/minecraft/resources/ResourceLocation;)V"))
    private void panoramaByeBye(TextureManager instance, ResourceLocation resource) {
        if (BaseEnv.available()) return;
        instance.bind(resource);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void render(PoseStack graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (BaseEnv.available()) NarutoRenderer.INSTANCE.renderFrame(graphics);
    }
}