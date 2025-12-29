package me.kall.narutoloading.mixin.util;

import me.kall.narutoloading.ext.IFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Frustum.class)
public abstract class FrustumMixin implements IFrustum {
    @Shadow protected abstract boolean cubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

    @Override
    public boolean naruto$isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.cubeInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
