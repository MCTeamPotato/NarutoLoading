package me.kall.narutoloading.network;

import me.kall.narutoloading.client.ui.NarutoScreen;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.client.WallManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void sync(@NotNull ListTag tags) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        ResourceLocation dimension = level.dimension().location();
        List<Wall> walls = new ArrayList<>(tags.size());

        for (int index = 0; index < tags.size(); index++) {
            Wall wall = Wall.fromTag(tags.getCompound(index), dimension);
            if (wall != null) walls.add(wall);
        }

        WallManager.applyRemote(walls);
        NarutoScreen.remoteChanged();
    }

    public static void put(@Nullable CompoundTag tag) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || tag == null) return;

        Wall wall = Wall.fromTag(tag, level.dimension().location());
        if (wall == null) return;

        WallManager.putRemote(wall);
        NarutoScreen.remoteChanged();
    }

    public static void remove(long @NotNull [] corners) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Wall wall = Wall.fromCorners(corners, level.dimension().location());
        if (wall == null) return;

        WallManager.remove(wall);
        NarutoScreen.remoteChanged();
    }

    public static void openConfig(@Nullable CompoundTag tag) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || tag == null) return;

        Wall wall = Wall.fromTag(tag, level.dimension().location());
        minecraft.setScreen(NarutoScreen.createForWall(minecraft.screen, wall));
    }
}
