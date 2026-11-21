package me.kall.narutoloading.mixin.substitution;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.impl.Substitution;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PanoramaRenderer.class)
public abstract class MixinPanoramaRenderer {
    @WrapMethod(method = "render")
    private void substitute(float deltaT, float alpha, Operation<Void> original) {
        if (Substitution.checkAndRun()) return;
        original.call(deltaT, alpha);
    }
}
