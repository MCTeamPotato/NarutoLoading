package me.kall.narutoloading.mixin.noworld.impl.screen;

import me.kall.narutoloading.common.env.BaseEnv;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen extends AbstractContainerEventHandler implements Renderable {
    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void dirtScreenByeBye(float partialTick, CallbackInfo ci) {
        if (BaseEnv.available()) {
            ci.cancel();
        }
    }
}
