package me.kall.narutoloading.mixin.substitution;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.client.overlay.Substitution;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.WinScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WinScreen.class)
public abstract class MixinWinScreen {
    @WrapMethod(method = "renderBg")
    private void substitute(GuiGraphics guiGraphics, Operation<Void> original) {
        if (Substitution.checkAndRun()) return;
        original.call(guiGraphics);
    }
}
