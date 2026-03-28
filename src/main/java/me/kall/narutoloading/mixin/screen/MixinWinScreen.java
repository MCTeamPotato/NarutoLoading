package me.kall.narutoloading.mixin.screen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.WinScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WinScreen.class)
public abstract class MixinWinScreen {
    @WrapMethod(method = "renderBg")
    private void skipRenderBg(GuiGraphics guiGraphics, Operation<Void> original) {
        NarutoRenderer.getInstance().renderFrame();
    }
}
