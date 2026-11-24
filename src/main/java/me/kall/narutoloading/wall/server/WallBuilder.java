package me.kall.narutoloading.wall.server;

import it.unimi.dsi.fastutil.longs.LongList;
import me.kall.narutoloading.network.message.WallPutMessage;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.data.WallStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WallBuilder {
    private static final int COOLDOWN_TICKS = 10;

    private static final Map<UUID, Corner> CORNERS = new HashMap<>();
    private static final Map<UUID, Long> LAST_CLICK = new HashMap<>();

    private WallBuilder() {}

    public static void select(@NotNull ServerPlayer player, @NotNull BlockPos pos, @NotNull String media) {
        long now = player.level().getGameTime();
        Long last = LAST_CLICK.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) return;
        LAST_CLICK.put(player.getUUID(), now);

        ResourceLocation dimension = player.serverLevel().dimension().location();
        Corner corner = CORNERS.get(player.getUUID());

        if (corner == null || !corner.dimension().equals(dimension)) {
            CORNERS.put(player.getUUID(), new Corner(dimension, pos));
            player.displayClientMessage(Component.translatable("narutoloading.message.wall.corner.first", pos.toShortString()), false);
            return;
        }

        CORNERS.remove(player.getUUID());
        player.displayClientMessage(Component.translatable("narutoloading.message.wall.corner.second", pos.toShortString()), false);

        Wall wall = build(player.serverLevel(), corner.pos(), pos, media);

        if (wall == null) player.displayClientMessage(Component.translatable("narutoloading.message.wall.invalid"), false);
        else player.displayClientMessage(Component.translatable("narutoloading.message.wall.built", corner.pos().toShortString(), pos.toShortString()), false);
    }

    public static @Nullable Wall build(@NotNull ServerLevel level, @NotNull BlockPos from, @NotNull BlockPos to, @NotNull String media) {
        Wall wall = find(level, from, to, media);
        if (wall == null) return null;

        WallStorage.get(level).add(wall);
        PacketDistributor.sendToPlayersInDimension(level, new WallPutMessage(wall.toTag()));
        return wall;
    }

    public static @Nullable Wall find(@NotNull ServerLevel level, @NotNull BlockPos bottom1, @NotNull BlockPos bottom2, @NotNull String media) {
        if (WallRules.isIllegal(media)) return null;

        LongList bottom;
        try {
            bottom = Wall.getLine(bottom1, bottom2);
        } catch (RuntimeException exception) {
            return null;
        }

        if (bottom.size() > ServerConfig.wallMaxSide()) return null;

        for (long position : bottom) {
            if (!WallRules.isFrame(level.getBlockState(BlockPos.of(position)))) return null;
        }

        int dx = bottom2.getX() - bottom1.getX();
        int dy = bottom2.getY() - bottom1.getY();
        int dz = bottom2.getZ() - bottom1.getZ();

        int maxHeight = Math.min(ServerConfig.wallMaxSide(), ServerConfig.wallMaxArea() / bottom.size());

        for (int hx = -1; hx <= 1; hx++) {
            for (int hy = -1; hy <= 1; hy++) {
                for (int hz = -1; hz <= 1; hz++) {
                    if (hx == 0 && hy == 0 && hz == 0) continue;
                    if (hx * dx + hy * dy + hz * dz != 0) continue;

                    int nx = dy * hz - dz * hy;
                    int ny = dz * hx - dx * hz;
                    int nz = dx * hy - dy * hx;
                    if (ny != 0 && !(nx == 0 && nz == 0)) continue;

                    BlockPos top1 = null;
                    BlockPos top2 = null;

                    for (int length = 1; length <= maxHeight; length++) {
                        BlockPos candidate1 = bottom1.offset(hx * length, hy * length, hz * length);
                        BlockPos candidate2 = bottom2.offset(hx * length, hy * length, hz * length);

                        if (!framed(level, bottom1, candidate1)) break;
                        if (!framed(level, bottom2, candidate2)) break;
                        if (!framed(level, candidate1, candidate2)) continue;

                        top1 = candidate1;
                        top2 = candidate2;
                    }

                    if (top1 == null) continue;

                    try {
                        return new Wall(bottom1, bottom2, top1, top2, level.dimension().location(), media);
                    } catch (IllegalArgumentException exception) {
                        return null;
                    }
                }
            }
        }

        return null;
    }

    public static void forget(@NotNull UUID player) {
        CORNERS.remove(player);
        LAST_CLICK.remove(player);
    }

    private static boolean framed(@NotNull ServerLevel level, @NotNull BlockPos from, @NotNull BlockPos to) {
        for (long position : Wall.getLine(from, to)) {
            if (!WallRules.isFrame(level.getBlockState(BlockPos.of(position)))) return false;
        }

        return true;
    }

    private record Corner(ResourceLocation dimension, BlockPos pos) {}
}
