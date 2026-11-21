package me.kall.narutoloading.mixin.unlock;

import com.mojang.blaze3d.platform.Window;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class MixinWindow {
    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void unlockFpsLimit(@NotNull CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(1000000);
    }
}
