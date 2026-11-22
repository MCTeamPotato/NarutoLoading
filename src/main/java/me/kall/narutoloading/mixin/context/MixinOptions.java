package me.kall.narutoloading.mixin.context;

import me.kall.narutoloading.client.ClientUiControl;
import me.kall.narutoloading.config.NarutoConfig;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class MixinOptions {
    @Inject(method = "getSoundSourceVolume", at = @At("HEAD"), cancellable = true)
    private void skipSound(@NotNull SoundSource category, CallbackInfoReturnable<Float> cir) {
        if (category.equals(SoundSource.MUSIC) && NarutoConfig.muteMusic() && ClientUiControl.active().isRunning() && ClientUiControl.active().hasAudio()) cir.setReturnValue(0.0F);
    }
}
