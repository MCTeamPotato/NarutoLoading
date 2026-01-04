package me.kall.narutoloading.mixin.inworld;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.common.LifetimeController;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow public abstract boolean isPaused();

    @Inject(method = "runTick", at = @At("TAIL"))
    private void onPause(CallbackInfo ci) {
        if (this.isPaused()) {
            for (ObjectSet<NarutoInWorldRenderer> renderers : ClientScreensRenderer.CLIENT_SCREENS.values()) {
                for (NarutoInWorldRenderer renderer : renderers) {
                    LifetimeController lifetimeController = renderer.lifetime;
                    if (lifetimeController == null) continue;
                    lifetimeController.pause();
                }
            }
        }
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void noLimit(@NotNull CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(114514);
    }
}
