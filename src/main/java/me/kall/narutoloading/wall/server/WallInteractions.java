package me.kall.narutoloading.wall.server;

import me.kall.narutoloading.network.NarutoNetwork;
import me.kall.narutoloading.network.message.OpenWallConfigMessage;
import me.kall.narutoloading.network.message.WallSyncMessage;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.data.WallStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public final class WallInteractions {
    private WallInteractions() {}

    /**
     * 潜行右键墙体：空手打开配置界面，手持建造物品选角。
     *
     * @return {@code true} 表示该次交互已被消费，调用方应取消事件
     */
    public static boolean interact(@NotNull ServerPlayer player, @NotNull BlockPos pos, @NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            Wall wall = WallStorage.get(player.serverLevel()).get(pos.asLong());
            if (wall == null) return false;

            NarutoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenWallConfigMessage(wall.toTag()));
            return true;
        }

        if (!WallRules.isBuilderItem(stack)) return false;

        WallBuilder.select(player, pos, "");
        return true;
    }

    public static void sync(@NotNull ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ListTag list = new ListTag();

        for (Wall wall : WallStorage.get(level).all()) list.add(wall.toTag());

        NarutoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WallSyncMessage(list));
    }
}
