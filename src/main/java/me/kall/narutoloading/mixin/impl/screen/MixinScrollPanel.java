package me.kall.narutoloading.mixin.impl.screen;

import com.mojang.blaze3d.vertex.Tesselator;
import me.kall.narutoloading.NarutoLoading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScrollPanel.class, remap = false)
public abstract class MixinScrollPanel {
    @Shadow @Final private Minecraft client;
    @Shadow protected abstract void drawGradientRect(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color1, int color2);
    @Shadow @Final protected int left;
    @Shadow @Final protected int right;
    @Shadow @Final protected int bottom;
    @Shadow @Final protected int top;
    @Shadow @Final private int bgColorFrom;
    @Shadow @Final private int bgColorTo;

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void dirtScreenByeBye(GuiGraphics guiGraphics, Tesselator tess, float partialTick, @NotNull CallbackInfo ci) {
        ci.cancel();

        if (this.client.level != null) {
            this.drawGradientRect(guiGraphics, this.left, this.top, this.right, this.bottom, bgColorFrom, bgColorTo);
        } else {
            NarutoLoading.RENDERER.renderFrame(guiGraphics);
        }
    }
}
