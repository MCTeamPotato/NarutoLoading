package me.kall.narutoloading.mixin.noworld.impl.screen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PanoramaRenderer.class)
public abstract class MixinPanoramaRenderer {
    @WrapMethod(method = "render")
    private void onRender(GuiGraphics guiGraphics, int width, int height, float fade, float partialTick, Operation<Void> original) {
        if (BaseEnv.available()) {
            NarutoRenderer.INSTANCE.renderFrame(guiGraphics);
        } else {
            original.call(guiGraphics, width, height, fade, partialTick);
        }
    }
}
