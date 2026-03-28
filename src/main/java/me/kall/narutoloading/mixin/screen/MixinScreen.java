package me.kall.narutoloading.mixin.screen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @SuppressWarnings("UnstableApiUsage")
    @WrapMethod(method = "renderDirtBackground")
    private void skipRenderDirtBackground(GuiGraphics guiGraphics, Operation<Void> original) {
        Screen screen = (Screen) (Object) this;
        NarutoRenderer.getInstance().renderFrame();
        MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered(screen, guiGraphics));
    }
}
