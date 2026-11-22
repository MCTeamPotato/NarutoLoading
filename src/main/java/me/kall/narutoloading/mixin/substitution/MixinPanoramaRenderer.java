package me.kall.narutoloading.mixin.substitution;

import me.kall.narutoloading.client.overlay.Substitution;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PanoramaRenderer.class)
public abstract class MixinPanoramaRenderer {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void substitute(CallbackInfo ci) {
        if (Substitution.checkAndRun()) ci.cancel();
    }
}
