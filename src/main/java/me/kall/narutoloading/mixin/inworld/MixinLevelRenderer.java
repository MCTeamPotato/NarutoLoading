package me.kall.narutoloading.mixin.inworld;

import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Inject(method = "allChanged", at = @At("TAIL"))
    private void onFinishReload(CallbackInfo ci) {
        ClientScreensRenderer.reload();
    }
}
