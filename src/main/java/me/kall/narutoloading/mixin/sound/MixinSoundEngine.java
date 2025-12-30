package me.kall.narutoloading.mixin.sound;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import me.kall.narutoloading.common.env.BaseEnv;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {
    @Inject(method = "reload", at = @At("HEAD"))
    private void shutdown(CallbackInfo ci) {
        if (BaseEnv.available() && NarutoRenderer.INSTANCE.audioExecutor != null) {
            NarutoRenderer.INSTANCE.audioExecutor.shutdown();
            NarutoLoading.LOGGER.info("Minecraft SoundEngine starts to load. Shutting down NarutoAudioExecutor for the OpenAL context synchronization.");
        }
    }

    @Inject(method = "reload", at = @At("TAIL"))
    private void setup(CallbackInfo ci) {
        if (BaseEnv.available() && NarutoRenderer.INSTANCE.lifetime != null) {
            NarutoRenderer.INSTANCE.lifetime.syncSoundEngine = true;
        }
    }
}
