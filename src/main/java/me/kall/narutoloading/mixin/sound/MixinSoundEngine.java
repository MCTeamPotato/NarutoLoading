package me.kall.narutoloading.mixin.sound;

import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.core.base.LifetimeController;
import me.kall.narutoloading.core.executor.audio.AbstractAudioExecutor;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {
    @Inject(method = "reload", at = @At("HEAD"))
    private void shutdown(CallbackInfo ci) {
        AbstractAudioExecutor audioExecutor = NarutoRenderer.getInstance().audioExecutor.get();
        if (audioExecutor == null) return;
        audioExecutor.shutdown();
    }

    @Inject(method = "reload", at = @At("TAIL"))
    private void setup(CallbackInfo ci) {
        LifetimeController lifetime = NarutoRenderer.getInstance().lifetime.get();
        if (lifetime == null) return;
        lifetime.syncSoundEngine.set(true);
    }
}
