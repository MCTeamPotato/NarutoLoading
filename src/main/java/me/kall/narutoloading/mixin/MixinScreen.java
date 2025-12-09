package me.kall.narutoloading.mixin;

import me.kall.narutoloading.NarutoLoading;
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
    @SuppressWarnings("UnstableApiUsage")
    @Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
    private void dirtScreenByeBye(GuiGraphics guiGraphics, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        NarutoLoading.RENDERER.renderFrame(guiGraphics);
        MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered(screen, guiGraphics));
        ci.cancel();
    }
}
