package me.kall.narutoloading.mixin.noworld.clear;

import me.kall.narutoloading.noworld.core.NarutoRenderer;
import me.kall.narutoloading.noworld.core.checker.FadeChecker;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public class MixinTextureManager {
    @Inject(method = "bind", at = @At("HEAD"), cancellable = true)
    private void onBind(ResourceLocation resource, CallbackInfo ci) {
        ResourceLocation texture = NarutoRenderer.INSTANCE.textureLocation;
        if (texture == null) return;
        if (FadeChecker.transparency() && !resource.equals(texture)) ci.cancel();
    }
}
