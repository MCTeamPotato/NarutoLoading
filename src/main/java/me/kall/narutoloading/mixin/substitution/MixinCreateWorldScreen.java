package me.kall.narutoloading.mixin.substitution;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.impl.Substitution;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CreateWorldScreen.class)
public abstract class MixinCreateWorldScreen {
    @WrapMethod(method = "renderDirtBackground")
    private void substitute(GuiGraphics guiGraphics, Operation<Void> original) {
        if (Substitution.checkAndRun()) return;
        original.call(guiGraphics);
    }
}
