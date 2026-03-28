package me.kall.narutoloading.mixin.element;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PanoramaRenderer.class)
public abstract class MixinPanoramaRenderer {
    @WrapMethod(method = "render")
    private void skipPanoramaRenderer(float deltaT, float alpha, Operation<Void> original) {
        NarutoRenderer.getInstance().renderFrame();
    }
}
