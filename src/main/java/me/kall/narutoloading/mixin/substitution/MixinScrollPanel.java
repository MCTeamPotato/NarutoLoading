package me.kall.narutoloading.mixin.substitution;

import com.mojang.blaze3d.vertex.Tesselator;
import me.kall.narutoloading.client.overlay.Substitution;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.gui.widget.ScrollPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScrollPanel.class, remap = false)
public abstract class MixinScrollPanel {
    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void skipDarkDirt(GuiGraphics graphics, Tesselator tesselator, float partialTick, CallbackInfo ci) {
        if (Substitution.check()) ci.cancel();
    }
}
