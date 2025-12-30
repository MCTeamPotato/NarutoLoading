package me.kall.narutoloading.mixin.sound;

import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
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
        if (category.equals(SoundSource.MUSIC) && NarutoRenderer.INSTANCE.isRunning() && BaseEnv.available()) {
            cir.setReturnValue(0.0F);
        }
    }
}
