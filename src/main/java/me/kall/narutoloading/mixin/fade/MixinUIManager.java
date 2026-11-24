package me.kall.narutoloading.mixin.fade;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.mc.UIManager;
import me.kall.narutoloading.client.fade.FadeCenter;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = UIManager.class, remap = false)
public abstract class MixinUIManager {
    @WrapMethod(method = "render")
    private void fade$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, @NotNull Operation<Void> original) {
        float alpha = FadeCenter.fadeAlpha();

        float[] color = RenderSystem.getShaderColor();

        if (alpha < 1.0F) RenderSystem.setShaderColor(color[0] * alpha, color[1] * alpha, color[2] * alpha, color[3] * alpha);

        try {
            original.call(graphics, mouseX, mouseY, partialTick);
        } finally {
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        }
    }
}
