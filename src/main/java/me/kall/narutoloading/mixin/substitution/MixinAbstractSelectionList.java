package me.kall.narutoloading.mixin.substitution;

import me.kall.narutoloading.client.overlay.Substitution;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {
    @Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true)
    private void substituteBackground(GuiGraphics graphics, CallbackInfo ci) {
        if (Substitution.check()) ci.cancel();
    }

    @Inject(method = "renderListSeparators", at = @At("HEAD"), cancellable = true)
    private void substituteSeparators(GuiGraphics graphics, CallbackInfo ci) {
        if (Substitution.check()) ci.cancel();
    }
}
