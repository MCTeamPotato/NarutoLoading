package me.kall.narutoloading.wall.geometry;

import me.kall.narutoloading.wall.Wall;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class WallGeometry {
    public record Coords(
            double normalX, double normalY, double normalZ,
            double bottomFromX, double bottomFromY, double bottomFromZ, float u0, float v0,
            double bottomToX, double bottomToY, double bottomToZ, float u1, float v1,
            double topToX, double topToY, double topToZ, float u2, float v2,
            double topFromX, double topFromY, double topFromZ, float u3, float v3
    ) {}

    private static final double OFFSET = 0.005D;

    private WallGeometry() {}

    @Contract("_, _ -> new")
    public static WallGeometry.@NotNull Coords computeCoords(@NotNull Wall wall, @NotNull Camera camera) {
        Vec3 camVec = camera.getPosition();
        Vector3f upVec = camera.getUpVector();

        double camX = camVec.x, camY = camVec.y, camZ = camVec.z;

        int minX = wall.minX, minY = wall.minY, minZ = wall.minZ;
        int maxX = wall.maxX, maxY = wall.maxY, maxZ = wall.maxZ;

        double[] corners = wall.sortedCorners(camX, camY, camZ, upVec);

        double bottomFromX = corners[0], bottomFromY = corners[1], bottomFromZ = corners[2];
        double bottomToX = corners[3], bottomToY = corners[4], bottomToZ = corners[5];
        double topFromX = corners[6], topFromY = corners[7], topFromZ = corners[8];
        double topToX = corners[9], topToY = corners[10], topToZ = corners[11];

        double normalX = 0, normalY = 0, normalZ = 0;
        switch (wall.axisThickness) {
            case X -> normalX = Math.abs(camX - minX) < Math.abs(camX - maxX) ? -1 : 1;
            case Y -> normalY = Math.abs(camY - minY) < Math.abs(camY - maxY) ? -1 : 1;
            case Z -> normalZ = Math.abs(camZ - minZ) < Math.abs(camZ - maxZ) ? -1 : 1;
        }

        double uDirX = bottomToX - bottomFromX, uDirY = bottomToY - bottomFromY, uDirZ = bottomToZ - bottomFromZ;
        double vDirX = topFromX - bottomFromX, vDirY = topFromY - bottomFromY, vDirZ = topFromZ - bottomFromZ;

        double uLength = Math.sqrt(uDirX * uDirX + uDirY * uDirY + uDirZ * uDirZ);
        double vLength = Math.sqrt(vDirX * vDirX + vDirY * vDirY + vDirZ * vDirZ);

        boolean isSquare = Math.abs(uLength - vLength) < 1e-6;

        if (uLength > 1e-9) { uDirX /= uLength; uDirY /= uLength; uDirZ /= uLength; }
        if (vLength > 1e-9) { vDirX /= vLength; vDirY /= vLength; vDirZ /= vLength; }

        double camUpX = upVec.x(), camUpY = upVec.y(), camUpZ = upVec.z();
        double dotUpN = camUpX * normalX + camUpY * normalY + camUpZ * normalZ;
        double upProjX = camUpX - normalX * dotUpN;
        double upProjY = camUpY - normalY * dotUpN;
        double upProjZ = camUpZ - normalZ * dotUpN;
        double upProjLenSq = upProjX * upProjX + upProjY * upProjY + upProjZ * upProjZ;

        if (upProjLenSq < 1e-5) {
            Vector3f lookVec = camera.getLookVector();
            double lookX = lookVec.x(), lookY = lookVec.y(), lookZ = lookVec.z();
            double dotLookN = lookX * normalX + lookY * normalY + lookZ * normalZ;
            upProjX = lookX - normalX * dotLookN;
            upProjY = lookY - normalY * dotLookN;
            upProjZ = lookZ - normalZ * dotLookN;
            upProjLenSq = upProjX * upProjX + upProjY * upProjY + upProjZ * upProjZ;

            if (upProjLenSq < 1e-5) {
                upProjX = uDirX; upProjY = uDirY; upProjZ = uDirZ;
                upProjLenSq = 1.0;
            }
        }

        double invUpLen = 1.0 / Math.sqrt(upProjLenSq);
        upProjX *= invUpLen; upProjY *= invUpLen; upProjZ *= invUpLen;

        double upDotU = upProjX * uDirX + upProjY * uDirY + upProjZ * uDirZ;
        double upDotV = upProjX * vDirX + upProjY * vDirY + upProjZ * vDirZ;

        double upX, upY, upZ, preRightX, preRightY, preRightZ;
        if (isSquare) {
            if (Math.abs(upDotU) >= Math.abs(upDotV)) {
                double sign = upDotU >= 0 ? 1.0 : -1.0;
                upX = uDirX * sign; upY = uDirY * sign; upZ = uDirZ * sign;
                preRightX = vDirX; preRightY = vDirY; preRightZ = vDirZ;
            } else {
                double sign = upDotV >= 0 ? 1.0 : -1.0;
                upX = vDirX * sign; upY = vDirY * sign; upZ = vDirZ * sign;
                preRightX = uDirX; preRightY = uDirY; preRightZ = uDirZ;
            }
        } else {
            double sign = upDotV >= 0 ? 1.0 : -1.0;
            upX = vDirX * sign; upY = vDirY * sign; upZ = vDirZ * sign;
            preRightX = uDirX; preRightY = uDirY; preRightZ = uDirZ;
        }

        double targetX = upY * normalZ - upZ * normalY;
        double targetY = upZ * normalX - upX * normalZ;
        double targetZ = upX * normalY - upY * normalX;

        double alignDot = preRightX * targetX + preRightY * targetY + preRightZ * targetZ;

        double rightX, rightY, rightZ;
        if (alignDot >= 0) {
            rightX = preRightX; rightY = preRightY; rightZ = preRightZ;
        } else {
            rightX = -preRightX; rightY = -preRightY; rightZ = -preRightZ;
        }

        Frame frame = new Frame(wall.centerX, wall.centerY, wall.centerZ, rightX, rightY, rightZ, upX, upY, upZ);

        float u0 = frame.u(bottomFromX, bottomFromY, bottomFromZ), v0 = frame.v(bottomFromX, bottomFromY, bottomFromZ);
        float u1 = frame.u(bottomToX, bottomToY, bottomToZ), v1 = frame.v(bottomToX, bottomToY, bottomToZ);
        float u2 = frame.u(topToX, topToY, topToZ), v2 = frame.v(topToX, topToY, topToZ);
        float u3 = frame.u(topFromX, topFromY, topFromZ), v3 = frame.v(topFromX, topFromY, topFromZ);

        return new Coords(
                normalX, normalY, normalZ,
                bottomFromX + normalX * OFFSET, bottomFromY + normalY * OFFSET, bottomFromZ + normalZ * OFFSET, u0, v0,
                bottomToX + normalX * OFFSET, bottomToY + normalY * OFFSET, bottomToZ + normalZ * OFFSET, u1, v1,
                topToX + normalX * OFFSET, topToY + normalY * OFFSET, topToZ + normalZ * OFFSET, u2, v2,
                topFromX + normalX * OFFSET, topFromY + normalY * OFFSET, topFromZ + normalZ * OFFSET, u3, v3
        );
    }

    private record Frame(
            double centerX, double centerY, double centerZ,
            double rightX, double rightY, double rightZ,
            double upX, double upY, double upZ
    ) {
        @Contract(pure = true)
        private float u(double x, double y, double z) {
            return ((x - this.centerX) * this.rightX + (y - this.centerY) * this.rightY + (z - this.centerZ) * this.rightZ) >= 0D ? 1.0F : 0.0F;
        }

        @Contract(pure = true)
        private float v(double x, double y, double z) {
            return ((x - this.centerX) * this.upX + (y - this.centerY) * this.upY + (z - this.centerZ) * this.upZ) >= 0D ? 0.0F : 1.0F;
        }
    }

    @Nullable
    public static Vec3 getIntersection(@NotNull Vec3 origin, @NotNull Vec3 direction, @NotNull Wall wall) {
        double leftBottomX = wall.leftBottom.getX();
        double leftBottomY = wall.leftBottom.getY();
        double leftBottomZ = wall.leftBottom.getZ();

        double rightBottomX = wall.rightBottom.getX();
        double rightBottomY = wall.rightBottom.getY();
        double rightBottomZ = wall.rightBottom.getZ();

        double leftTopX = wall.leftTop.getX();
        double leftTopY = wall.leftTop.getY();
        double leftTopZ = wall.leftTop.getZ();

        double originX = origin.x;
        double originY = origin.y;
        double originZ = origin.z;

        double directionX = direction.x;
        double directionY = direction.y;
        double directionZ = direction.z;

        double edge1X = rightBottomX - leftBottomX;
        double edge1Y = rightBottomY - leftBottomY;
        double edge1Z = rightBottomZ - leftBottomZ;

        double edge2X = leftTopX - leftBottomX;
        double edge2Y = leftTopY - leftBottomY;
        double edge2Z = leftTopZ - leftBottomZ;

        double normalX = edge1Y * edge2Z - edge1Z * edge2Y;
        double normalY = edge1Z * edge2X - edge1X * edge2Z;
        double normalZ = edge1X * edge2Y - edge1Y * edge2X;

        double normalLength = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (normalLength < 1e-8) return null;

        normalX = normalX * (1.0 / normalLength);
        normalY = normalY * (1.0 / normalLength);
        normalZ = normalZ * (1.0 / normalLength);

        double denominator = normalX * directionX + normalY * directionY + normalZ * directionZ;
        if (Math.abs(denominator) < 1e-8) return null;

        double hitDist = ((leftBottomX - originX) * normalX + (leftBottomY - originY) * normalY + (leftBottomZ - originZ) * normalZ) / denominator;
        if (hitDist < 0) return null;

        double hitX = originX + directionX * hitDist;
        double hitY = originY + directionY * hitDist;
        double hitZ = originZ + directionZ * hitDist;

        double rightVecX = rightBottomX - leftBottomX;
        double rightVecY = rightBottomY - leftBottomY;
        double rightVecZ = rightBottomZ - leftBottomZ;

        double rightVecLength = rightVecX * rightVecX + rightVecY * rightVecY + rightVecZ * rightVecZ;

        double upVecX = leftTopX - leftBottomX;
        double upVecY = leftTopY - leftBottomY;
        double upVecZ = leftTopZ - leftBottomZ;

        double upVecLength = upVecX * upVecX + upVecY * upVecY + upVecZ * upVecZ;

        double toHitX = hitX - leftBottomX;
        double toHitY = hitY - leftBottomY;
        double toHitZ = hitZ - leftBottomZ;

        double localU = (toHitX * rightVecX + toHitY * rightVecY + toHitZ * rightVecZ) / rightVecLength;
        double localV = (toHitX * upVecX + toHitY * upVecY + toHitZ * upVecZ) / upVecLength;

        return localU >= 0 && localU <= 1 && localV >= 0 && localV <= 1 ? new Vec3(hitX, hitY, hitZ) : null;
    }
}
