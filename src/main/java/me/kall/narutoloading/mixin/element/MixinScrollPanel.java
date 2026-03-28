package me.kall.narutoloading.mixin.element;

import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScrollPanel.class, remap = false)
public class MixinScrollPanel {
    @Inject(method = "drawBackground", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V", ordinal = 0), cancellable = true)
    private void skipDrawBackground(@NotNull CallbackInfo ci) {
        ci.cancel();
        NarutoRenderer.getInstance().renderFrame();
    }
}
