package me.kall.narutoloading.mixin.substitution.mui;

import icyllis.modernui.mc.BlurHandler;
import me.kall.narutoloading.client.overlay.Substitution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlurHandler.class, remap = false)
public abstract class MixinBlurHandler {
    @Inject(method = "drawScreenBackground", at = @At("HEAD"), cancellable = true)
    private void substitute(CallbackInfo ci) {
        if (Substitution.checkAndRun()) ci.cancel();
    }
}
