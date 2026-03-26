package me.kall.narutoloading.mixin.context;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.kall.narutoloading.NarutoLoading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @WrapOperation(method = "render", at = @At(value = "NEW", target = "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Lnet/minecraft/client/gui/GuiGraphics;"))
    private GuiGraphics capture(Minecraft minecraft, MultiBufferSource.BufferSource bufferSource, @NotNull Operation<GuiGraphics> original) {
        GuiGraphics graphics = original.call(minecraft, bufferSource);
        NarutoLoading.GUI_GRAPHICS.set(graphics);
        return graphics;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void clean(CallbackInfo ci) {
        NarutoLoading.GUI_GRAPHICS.remove();
    }
}
