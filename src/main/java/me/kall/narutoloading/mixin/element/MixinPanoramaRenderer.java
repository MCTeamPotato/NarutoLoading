package me.kall.narutoloading.mixin.element;

import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PanoramaRenderer.class)
public abstract class MixinPanoramaRenderer {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void skipPanoramaRenderer(CallbackInfo ci) {
        if (NarutoRenderer.getInstance().isRunning()) ci.cancel();
        NarutoRenderer.getInstance().renderFrame();
    }
}
