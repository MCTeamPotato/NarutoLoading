package me.kall.narutoloading.mixin.impl.screen;

import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen extends AbstractContainerEventHandler implements Renderable {
    @SuppressWarnings("UnstableApiUsage")
    @Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
    private void dirtScreenByeBye(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (NarutoRenderer.INSTANCE.ffmpegProvider.available()) {
            Screen screen = (Screen) (Object) this;
            NarutoRenderer.INSTANCE.renderFrame(guiGraphics);
            MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered(screen, guiGraphics));
            ci.cancel();
        }
    }
}
