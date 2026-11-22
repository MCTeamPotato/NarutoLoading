package me.kall.narutoloading.mixin.substitution.mui;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import icyllis.modernui.mc.BlurHandler;
import me.kall.narutoloading.client.Substitution;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BlurHandler.class, remap = false)
public abstract class MixinBlurHandler {
    @WrapMethod(method = "drawScreenBackground")
    private void substitute(GuiGraphics gr, int x1, int y1, int x2, int y2, Operation<Void> original) {
        if (Substitution.checkAndRun()) return;
        original.call(gr, x1, y1, x2, y2);
    }
}
