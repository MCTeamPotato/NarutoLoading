package me.kall.narutoloading.mixin.inworld;

import me.kall.narutoloading.inworld.ext.IFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Frustum.class)
public abstract class MixinFrustum implements IFrustum {
    @Shadow protected abstract int cubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

    @Override
    public boolean naruto$isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int i = this.cubeInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
        return i == -2 || i == -1;
    }
}
