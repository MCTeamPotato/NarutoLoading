package me.kall.narutoloading.mixin;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void limitless(@NotNull CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Minecraft.getInstance().getWindow().getFramerateLimit());
    }
}
