package me.kall.narutoloading.server;

import me.kall.narutoloading.network.NarutoNetwork;
import me.kall.narutoloading.network.message.WallRemoveMessage;
import me.kall.narutoloading.world.Wall;
import me.kall.narutoloading.world.data.WallStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WallValidation {
    private static final int INTERVAL_TICKS = 40;
    private static final int CHECKS_PER_TICK = 256;

    private static final Map<ResourceLocation, Queue> QUEUES = new HashMap<>();

    private static int interval;

    private WallValidation() {}

    public static void tick(@NotNull MinecraftServer server) {
        if (++interval >= INTERVAL_TICKS) {
            interval = 0;
            rebuild(server);
        }

        int budget = CHECKS_PER_TICK;

        for (ServerLevel level : server.getAllLevels()) {
            if (budget <= 0) return;

            Queue queue = QUEUES.get(level.dimension().location());
            if (queue != null && !queue.borders.isEmpty()) budget = queue.check(level, budget);
        }
    }

    private static void rebuild(@NotNull MinecraftServer server) {
        QUEUES.clear();

        for (ServerLevel level : server.getAllLevels()) {
            Queue queue = new Queue();

            for (Wall wall : WallStorage.get(level).all()) {
                for (long position : wall.borderInvolved()) queue.borders.add(new Border(wall, position));
            }

            QUEUES.put(level.dimension().location(), queue);
        }
    }

    private record Border(Wall wall, long position) {}

    private static final class Queue {
        private final List<Border> borders = new ArrayList<>();
        private int cursor;

        private int check(@NotNull ServerLevel level, int budget) {
            WallStorage storage = WallStorage.get(level);

            while (budget > 0) {
                if (this.cursor >= this.borders.size()) this.cursor = 0;

                Border border = this.borders.get(this.cursor++);
                budget--;

                if (storage.get(border.position()) != border.wall()) continue;

                BlockPos pos = BlockPos.of(border.position());
                if (!level.isLoaded(pos)) continue;
                if (WallRules.isFrame(level.getBlockState(pos))) continue;

                storage.remove(border.wall());
                NarutoNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new WallRemoveMessage(border.wall().toLongArray()));
            }

            if (this.cursor >= this.borders.size()) this.cursor = 0;
            return budget;
        }
    }
}
