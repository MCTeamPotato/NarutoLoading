package me.kall.narutoloading.world.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.kall.narutoloading.world.Wall;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WallStorage extends SavedData {
    public static final String DATA_NAME = "NarutoLoadingWalls";
    private static final String WALLS_KEY = "Walls";

    private final ResourceLocation dimension;
    private final Map<Wall, Wall> walls = new LinkedHashMap<>();
    private final Long2ObjectOpenHashMap<Wall> positions = new Long2ObjectOpenHashMap<>();

    private WallStorage(@NotNull ResourceLocation dimension) {
        this.dimension = dimension;
    }

    public static @NotNull WallStorage get(@NotNull ServerLevel level) {
        ResourceLocation dimension = level.dimension().location();

        return level.getDataStorage().computeIfAbsent(
                tag -> load(dimension, tag),
                () -> new WallStorage(dimension),
                DATA_NAME
        );
    }

    public void add(@NotNull Wall wall) {
        this.install(wall);
        this.setDirty();
    }

    public void update(@NotNull Wall wall) {
        Wall previous = this.walls.remove(wall);
        if (previous != null) this.unindex(previous);

        this.install(wall);
        this.setDirty();
    }

    public void remove(@NotNull Wall wall) {
        Wall previous = this.walls.remove(wall);
        if (previous == null) return;

        this.unindex(previous);
        this.setDirty();
    }

    public @Nullable Wall get(long position) {
        return this.positions.get(position);
    }

    public @Nullable Wall get(long @NotNull [] corners) {
        Wall key = Wall.fromCorners(corners, this.dimension);
        return key == null ? null : this.walls.get(key);
    }

    public @NotNull List<Wall> all() {
        return new ArrayList<>(this.walls.values());
    }

    private void install(@NotNull Wall wall) {
        this.walls.put(wall, wall);

        for (long position : wall.areaInvolved()) {
            if (!this.positions.containsKey(position)) this.positions.put(position, wall);
        }
    }

    private void unindex(@NotNull Wall wall) {
        for (long position : wall.areaInvolved()) {
            if (this.positions.get(position) == wall) this.positions.remove(position);
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (Wall wall : this.walls.values()) list.add(wall.toTag());

        tag.put(WALLS_KEY, list);
        return tag;
    }

    private static @NotNull WallStorage load(@NotNull ResourceLocation dimension, @NotNull CompoundTag tag) {
        WallStorage storage = new WallStorage(dimension);

        ListTag list = tag.getList(WALLS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            Wall wall = Wall.fromTag(list.getCompound(index), dimension);
            if (wall != null) storage.install(wall);
        }

        return storage;
    }
}
