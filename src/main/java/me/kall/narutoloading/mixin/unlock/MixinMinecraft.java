package me.kall.narutoloading.mixin.unlock;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void unlockFpsLimit(@NotNull CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(1000000);
    }
}
