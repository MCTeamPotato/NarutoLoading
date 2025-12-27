package me.kall.narutoloading.mixin.impl.sound;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {
    @Inject(method = "reload", at = @At("HEAD"))
    private void shutdown(CallbackInfo ci) {
        if (NarutoRenderer.INSTANCE.ffmpegProvider.available()) {
            NarutoRenderer.INSTANCE.audioExecutor.shutdown();
            NarutoLoading.LOGGER.info("Minecraft SoundEngine starts to load. Shutting down NarutoAudioExecutor for the OpenAL context synchronization.");
        }
    }

    @Inject(method = "reload", at = @At("TAIL"))
    private void setup(CallbackInfo ci) {
        if (NarutoRenderer.INSTANCE.ffmpegProvider.available()) {
            NarutoRenderer.INSTANCE.lifetime.setSyncSoundEngine(true);
        }
    }
}
