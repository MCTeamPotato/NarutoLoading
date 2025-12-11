package me.kall.narutoloading.mixin.clear;

import me.kall.narutoloading.render.MouseChecker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.Font$StringRenderOutput")
public abstract class MixinStringRenderOutput {
    @Mutable @Shadow @Final private float a;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void narutoInit(CallbackInfo ci) {
        this.a *= MouseChecker.fadeAlpha;
    }
}
