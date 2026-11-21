package me.kall.narutoloading.mixin.unlock;

import me.kall.narutoloading.impl.ClientUiControl;
import me.kall.narutoloading.produce.video.AbstractFrameProducer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void unlockFpsLimit(@NotNull CallbackInfoReturnable<Integer> cir) {
        if (ClientUiControl.active().isRunning()) {
            AbstractFrameProducer<?> video = ClientUiControl.active().video.get();
            if (video != null) cir.setReturnValue((int) Math.max(cir.getReturnValue().doubleValue(), video.args.fps()));
        }
    }
}