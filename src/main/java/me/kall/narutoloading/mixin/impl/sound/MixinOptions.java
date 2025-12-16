package me.kall.narutoloading.mixin.impl.sound;

import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.data.FFmpeg;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class MixinOptions {
    @Inject(method = "getSoundSourceVolume", at = @At("HEAD"), cancellable = true)
    private void sound(SoundSource category, CallbackInfoReturnable<Float> cir) {
        if (category.equals(SoundSource.MUSIC) && NarutoRenderer.INSTANCE.isRunning() && FFmpeg.available()) cir.setReturnValue(0.0F);
    }
}
