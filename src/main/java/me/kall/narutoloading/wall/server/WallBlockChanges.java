package me.kall.narutoloading.wall.server;

import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.narutoloading.network.message.WallRemoveMessage;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.data.WallStorage;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public final class WallBlockChanges {
    @SubscribeEvent
    public void blockChange(@NotNull BlockChangeEvent event) {
        ServerLevel level = event.level();
        WallStorage storage = WallStorage.get(level);
        Wall wall = storage.get(event.blockPos());

        if (wall == null || !wall.borderInvolved().contains(event.blockPos())) return;
        if (WallRules.isFrame(event.newState())) return;

        storage.remove(wall);
        PacketDistributor.sendToPlayersInDimension(level, new WallRemoveMessage(wall.toLongArray()));
    }
}
