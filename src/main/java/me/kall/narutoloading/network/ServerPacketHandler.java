package me.kall.narutoloading.network;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutoloading.network.message.WallPutMessage;
import me.kall.narutoloading.network.message.WallRemoveMessage;
import me.kall.narutoloading.server.ServerConfig;
import me.kall.narutoloading.server.WallRules;
import me.kall.narutoloading.world.Wall;
import me.kall.narutoloading.world.data.WallStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ServerPacketHandler {
    private static final Logger LOGGER = LogManager.getLogger("NarutoLoading");

    private ServerPacketHandler() {}

    public static void update(@Nullable ServerPlayer player, long @NotNull [] corners, @NotNull String media, float volume, boolean light) {
        if (player == null) return;

        ServerLevel level = player.serverLevel();
        WallStorage storage = WallStorage.get(level);
        Wall wall = storage.get(corners);

        if (wall == null) {
            reject(player, "update of an unknown wall");
            return;
        }

        if (!WallRules.canEdit(player, wall)) {
            reject(player, "update out of range");
            return;
        }

        if (!WallRules.validMedia(media)) {
            reject(player, "update with an invalid media name");
            return;
        }

        wall.media = media;
        wall.volume = Mth.clamp(volume, 0.0F, 2.0F);
        wall.light = light;
        storage.update(wall);

        NarutoNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new WallPutMessage(wall.toTag()));
    }

    public static void remove(@Nullable ServerPlayer player, long @NotNull [] corners) {
        if (player == null) return;

        ServerLevel level = player.serverLevel();
        WallStorage storage = WallStorage.get(level);
        Wall wall = storage.get(corners);

        if (wall == null) {
            reject(player, "removal of an unknown wall");
            return;
        }

        if (!WallRules.canEdit(player, wall)) {
            reject(player, "removal out of range");
            return;
        }

        storage.remove(wall);
        NarutoNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new WallRemoveMessage(wall.toLongArray()));
    }

    public static void clean(@Nullable ServerPlayer player, long @NotNull [] corners) {
        if (player == null) return;

        ServerLevel level = player.serverLevel();
        WallStorage storage = WallStorage.get(level);
        Wall wall = storage.get(corners);

        if (wall == null) {
            reject(player, "cleaning of an unknown wall");
            return;
        }

        if (!WallRules.canEdit(player, wall)) {
            reject(player, "cleaning out of range");
            return;
        }

        if (wall.areaInvolved().size() > ServerConfig.wallMaxArea()) {
            reject(player, "cleaning an oversized wall");
            return;
        }

        LongSet border = wall.borderInvolved();

        for (long position : wall.areaInvolved()) {
            if (border.contains(position)) continue;
            level.destroyBlock(BlockPos.of(position), true, player);
        }
    }

    private static void reject(@NotNull ServerPlayer player, @NotNull String action) {
        LOGGER.debug("Rejected wall {} from {} at {}", action, player.getGameProfile().getName(), player.blockPosition());
    }
}
