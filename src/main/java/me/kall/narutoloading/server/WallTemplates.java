package me.kall.narutoloading.server;

import me.kall.narutoloading.network.NarutoNetwork;
import me.kall.narutoloading.network.message.WallPutMessage;
import me.kall.narutoloading.world.Wall;
import me.kall.narutoloading.world.data.WallStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class WallTemplates {
    public static final String WALLS_KEY = "NarutoLoadingWalls";

    private static final Logger LOGGER = LogManager.getLogger("NarutoLoading");

    private WallTemplates() {}

    public static @NotNull List<CompoundTag> collect(@NotNull ServerLevel level, @NotNull BlockPos min, @NotNull BlockPos max) {
        List<CompoundTag> walls = new ArrayList<>();

        for (Wall wall : WallStorage.get(level).all()) {
            if (wall.minX < min.getX() || wall.minY < min.getY() || wall.minZ < min.getZ()) continue;
            if (wall.maxX > max.getX() + 1 || wall.maxY > max.getY() + 1 || wall.maxZ > max.getZ() + 1) continue;

            walls.add(relative(wall, min));
        }

        return walls;
    }

    public static void apply(@NotNull ServerLevel level, @NotNull List<CompoundTag> tags) {
        WallStorage storage = WallStorage.get(level);
        ResourceLocation dimension = level.dimension().location();

        for (CompoundTag tag : tags) {
            Wall wall = Wall.fromTag(tag, dimension);

            if (wall == null) {
                LOGGER.warn("Skipping a malformed wall placed by a structure template in {}", dimension);
                continue;
            }

            if (storage.get(wall.toLongArray()) == null) storage.add(wall);
            else storage.update(wall);

            NarutoNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new WallPutMessage(wall.toTag()));
        }
    }

    private static @NotNull CompoundTag relative(@NotNull Wall wall, @NotNull BlockPos origin) {
        long[] corners = wall.toLongArray();

        for (int index = 0; index < corners.length; index++) {
            corners[index] = BlockPos.of(corners[index]).subtract(origin).asLong();
        }

        CompoundTag tag = new CompoundTag();
        tag.putLongArray(Wall.CORNERS_KEY, corners);
        tag.putString(Wall.MEDIA_KEY, wall.media);
        tag.putFloat(Wall.VOLUME_KEY, wall.volume);
        tag.putBoolean(Wall.LIGHT_KEY, wall.light);
        return tag;
    }
}
