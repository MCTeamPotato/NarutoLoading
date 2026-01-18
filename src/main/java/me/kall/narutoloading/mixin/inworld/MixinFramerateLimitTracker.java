package me.kall.narutoloading.mixin.inworld;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class MixinFramerateLimitTracker {
    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void noLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(114514);
    }
}
