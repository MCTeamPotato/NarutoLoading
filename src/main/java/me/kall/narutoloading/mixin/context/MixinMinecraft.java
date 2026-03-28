package me.kall.narutoloading.mixin.context;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V"))
    private void stopEarlyRenderer(Minecraft instance, Overlay loadingGui, @NotNull Operation<Void> original) {
        System.setProperty("narutoloading.shutdown", "true");
        for (int i = 0; i < 20; i++) {
            System.err.println("Shutdown now");
        }
        original.call(instance, loadingGui);
    }
}
