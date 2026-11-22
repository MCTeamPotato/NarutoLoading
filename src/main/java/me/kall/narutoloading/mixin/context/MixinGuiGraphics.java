package me.kall.narutoloading.mixin.context;

import me.kall.narutoloading.client.context.Graphics;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphics {
    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        Graphics.set((GuiGraphics) (Object) this);
    }

    @Inject(method = "flush", at = @At("RETURN"))
    private void releaseGuiGraphics(CallbackInfo ci) {
        Graphics.set(null);
    }
}