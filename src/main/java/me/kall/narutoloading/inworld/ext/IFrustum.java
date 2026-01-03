package me.kall.narutoloading.inworld.ext;

import me.kall.narutoloading.inworld.core.InWorldScreen;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.NotNull;

public interface IFrustum {
    boolean naruto$isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

    static boolean isVisible(Frustum frustum, @NotNull InWorldScreen screen) {
        double minX = Math.min(screen.leftBottomCorner().getX(), screen.rightTopCorner().getX());
        double minY = Math.min(screen.leftBottomCorner().getY(), screen.rightTopCorner().getY());
        double minZ = Math.min(screen.leftBottomCorner().getZ(), screen.rightTopCorner().getZ());
        double maxX = Math.max(screen.leftBottomCorner().getX(), screen.rightTopCorner().getX()) + 1;
        double maxY = Math.max(screen.leftBottomCorner().getY(), screen.rightTopCorner().getY()) + 1;
        double maxZ = Math.max(screen.leftBottomCorner().getZ(), screen.rightTopCorner().getZ()) + 1;

        return ((IFrustum)frustum).naruto$isVisible(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
