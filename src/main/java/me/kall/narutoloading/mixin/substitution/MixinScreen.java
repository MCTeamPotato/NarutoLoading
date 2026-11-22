package me.kall.narutoloading.mixin.substitution;

import me.kall.narutoloading.client.overlay.Substitution;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
    private void substitute(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!Substitution.checkAndRun()) return;

        MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered((Screen) (Object) this, guiGraphics));
        ci.cancel();
    }
}
