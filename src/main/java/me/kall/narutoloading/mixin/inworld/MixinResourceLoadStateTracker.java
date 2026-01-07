package me.kall.narutoloading.mixin.inworld;

import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourceLoadStateTracker.class)
public abstract class MixinResourceLoadStateTracker {
    @Inject(method = "finishReload", at = @At("TAIL"))
    private void onFinishReload(CallbackInfo ci) {
        ClientScreensRenderer.reload();
    }
}
