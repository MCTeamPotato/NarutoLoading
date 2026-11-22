package me.kall.narutoloading.mixin.substitution;

import me.kall.narutoloading.client.Substitution;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScrollPanel.class)
public abstract class MixinScrollPanel {
    @Inject(method = "drawBackground", remap = false, at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"), cancellable = true)
    private void skipDarkDirt(CallbackInfo ci) {
        if (Substitution.check()) ci.cancel();
    }
}
