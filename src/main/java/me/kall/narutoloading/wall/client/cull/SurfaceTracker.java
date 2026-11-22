package me.kall.narutoloading.wall.client.cull;

import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.wall.geometry.WallGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class SurfaceTracker {
    private static final double SAME_SURFACE = 0.01D;
    private static final int LIFETIME_FRAMES = 2;

    private static final Map<WallGeometry.Coords, Integer> LIVE = new HashMap<>();

    private static volatile Snapshot snapshot = Snapshot.EMPTY;

    private static @Nullable ResourceLocation dimension;

    private SurfaceTracker() {}

    public static void track(@NotNull ResourceLocation dimension, @NotNull WallGeometry.Coords coords) {
        if (!NarutoConfig.cullFaces()) return;

        if (!dimension.equals(SurfaceTracker.dimension)) reset(dimension);

        Integer age = LIVE.put(coords, LIFETIME_FRAMES);
        if (age != null) return;

        publish();
        refresh(coords);
    }

    public static boolean hidesFace(@NotNull BlockPos pos, @NotNull Direction facing) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return false;

        double centerX = (double) pos.getX() + 0.5D + (double) facing.getStepX() * 0.5D;
        double centerY = (double) pos.getY() + 0.5D + (double) facing.getStepY() * 0.5D;
        double centerZ = (double) pos.getZ() + 0.5D + (double) facing.getStepZ() * 0.5D;

        return hides(level.dimension().location(), centerX, centerY, centerZ);
    }

    public static boolean hides(@NotNull ResourceLocation dimension, double x, double y, double z) {
        Snapshot current = snapshot;
        Surface[] surfaces = current.surfaces;

        if (surfaces.length == 0 || !dimension.equals(current.dimension)) return false;

        for (Surface surface : surfaces) {
            if (x < surface.minX || x > surface.maxX) continue;
            if (y < surface.minY || y > surface.maxY) continue;
            if (z < surface.minZ || z > surface.maxZ) continue;

            if (isIn(surface.coords, x, y, z)) return true;
        }

        return false;
    }

    public static void tick() {
        if (!NarutoConfig.cullFaces()) {
            if (!LIVE.isEmpty()) clear();
            return;
        }

        if (LIVE.isEmpty()) return;

        List<WallGeometry.Coords> expired = null;
        Iterator<Map.Entry<WallGeometry.Coords, Integer>> iterator = LIVE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<WallGeometry.Coords, Integer> entry = iterator.next();
            int age = entry.getValue() - 1;

            if (age <= 0) {
                if (expired == null) expired = new ArrayList<>();
                expired.add(entry.getKey());
                iterator.remove();
            } else {
                entry.setValue(age);
            }
        }

        if (expired == null) return;

        publish();
        for (WallGeometry.Coords coords : expired) refresh(coords);
    }

    private static void reset(@NotNull ResourceLocation dimension) {
        SurfaceTracker.dimension = dimension;
        LIVE.clear();
        snapshot = Snapshot.EMPTY;
    }

    private static void clear() {
        List<WallGeometry.Coords> surfaces = new ArrayList<>(LIVE.keySet());

        LIVE.clear();
        publish();

        for (WallGeometry.Coords coords : surfaces) refresh(coords);
    }

    private static void publish() {
        Surface[] surfaces = new Surface[LIVE.size()];

        int index = 0;
        for (WallGeometry.Coords coords : LIVE.keySet()) surfaces[index++] = new Surface(coords);

        snapshot = new Snapshot(dimension, surfaces);
    }

    private static void refresh(@NotNull WallGeometry.Coords coords) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        double minX = Math.min(coords.bottomFromX(), Math.min(coords.bottomToX(), coords.topFromX()));
        double minY = Math.min(coords.bottomFromY(), Math.min(coords.bottomToY(), coords.topFromY()));
        double minZ = Math.min(coords.bottomFromZ(), Math.min(coords.bottomToZ(), coords.topFromZ()));
        double maxX = Math.max(coords.bottomFromX(), Math.max(coords.bottomToX(), coords.topFromX()));
        double maxY = Math.max(coords.bottomFromY(), Math.max(coords.bottomToY(), coords.topFromY()));
        double maxZ = Math.max(coords.bottomFromZ(), Math.max(coords.bottomToZ(), coords.topFromZ()));

        minecraft.levelRenderer.setBlocksDirty((int) Math.floor(minX - 1D), (int) Math.floor(minY - 1D), (int) Math.floor(minZ - 1D), (int) Math.ceil(maxX + 1D), (int) Math.ceil(maxY + 1D), (int) Math.ceil(maxZ + 1D));
    }

    private static boolean isIn(@NotNull WallGeometry.Coords coords, double pointX, double pointY, double pointZ) {
        double bottomFromX = coords.bottomFromX(), bottomFromY = coords.bottomFromY(), bottomFromZ = coords.bottomFromZ();
        double bottomToX = coords.bottomToX(), bottomToY = coords.bottomToY(), bottomToZ = coords.bottomToZ();
        double topFromX = coords.topFromX(), topFromY = coords.topFromY(), topFromZ = coords.topFromZ();

        double edgeBottomX = bottomToX - bottomFromX, edgeBottomY = bottomToY - bottomFromY, edgeBottomZ = bottomToZ - bottomFromZ;
        double edgeSideX = topFromX - bottomFromX, edgeSideY = topFromY - bottomFromY, edgeSideZ = topFromZ - bottomFromZ;

        double normalX = coords.normalX(), normalY = coords.normalY(), normalZ = coords.normalZ();
        double normalLenSq = Mth.square(normalX) + Mth.square(normalY) + Mth.square(normalZ);

        double dotNormal = (pointX - bottomFromX) * normalX + (pointY - bottomFromY) * normalY + (pointZ - bottomFromZ) * normalZ;

        if (Math.abs(dotNormal / Math.sqrt(normalLenSq)) >= SAME_SURFACE) return false;

        double factor = dotNormal / normalLenSq;

        double toProjX = pointX - factor * normalX - bottomFromX;
        double toProjY = pointY - factor * normalY - bottomFromY;
        double toProjZ = pointZ - factor * normalZ - bottomFromZ;

        double paramU = Math.max(0D, Math.min(1D, (toProjX * edgeBottomX + toProjY * edgeBottomY + toProjZ * edgeBottomZ) / (edgeBottomX * edgeBottomX + edgeBottomY * edgeBottomY + edgeBottomZ * edgeBottomZ)));
        double paramV = Math.max(0D, Math.min(1D, (toProjX * edgeSideX + toProjY * edgeSideY + toProjZ * edgeSideZ) / (edgeSideX * edgeSideX + edgeSideY * edgeSideY + edgeSideZ * edgeSideZ)));

        double xDistSq = Mth.square(pointX - (bottomFromX + paramU * edgeBottomX + paramV * edgeSideX));
        double yDistSq = Mth.square(pointY - (bottomFromY + paramU * edgeBottomY + paramV * edgeSideY));
        double zDistSq = Mth.square(pointZ - (bottomFromZ + paramU * edgeBottomZ + paramV * edgeSideZ));

        return xDistSq + yDistSq + zDistSq < Mth.square(SAME_SURFACE);
    }

    private record Snapshot(@Nullable ResourceLocation dimension, Surface[] surfaces) {
        private static final Snapshot EMPTY = new Snapshot(null, new Surface[0]);
    }

    private static final class Surface {
        private final double minX, minY, minZ, maxX, maxY, maxZ;
        private final WallGeometry.Coords coords;

        private Surface(@NotNull WallGeometry.Coords coords) {
            this.coords = coords;

            this.minX = Math.min(coords.bottomFromX(), Math.min(coords.bottomToX(), coords.topFromX()));
            this.minY = Math.min(coords.bottomFromY(), Math.min(coords.bottomToY(), coords.topFromY()));
            this.minZ = Math.min(coords.bottomFromZ(), Math.min(coords.bottomToZ(), coords.topFromZ()));
            this.maxX = Math.max(coords.bottomFromX(), Math.max(coords.bottomToX(), coords.topFromX()));
            this.maxY = Math.max(coords.bottomFromY(), Math.max(coords.bottomToY(), coords.topFromY()));
            this.maxZ = Math.max(coords.bottomFromZ(), Math.max(coords.bottomToZ(), coords.topFromZ()));
        }
    }
}
