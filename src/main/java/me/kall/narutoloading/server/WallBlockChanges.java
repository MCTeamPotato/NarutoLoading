package me.kall.narutoloading.server;

import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.narutoloading.network.NarutoNetwork;
import me.kall.narutoloading.network.message.WallRemoveMessage;
import me.kall.narutoloading.world.Wall;
import me.kall.narutoloading.world.data.WallStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
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
        NarutoNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new WallRemoveMessage(wall.toLongArray()));
    }
}
