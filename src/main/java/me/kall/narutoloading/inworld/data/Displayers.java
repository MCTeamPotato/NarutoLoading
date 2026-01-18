package me.kall.narutoloading.inworld.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.duplicationless.data.ChunkData;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.duplicationless.util.Executor;
import me.kall.duplicationless.util.Positions;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.init.NarutoBlocks;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

@EventBusSubscriber(modid = NarutoLoading.MOD_ID)
public class Displayers extends ChunkData.BlockData {
    private final Object2ObjectMap<Identifier, Long2ObjectMap<Set<Long>>> data = new Object2ObjectOpenHashMap<>();

    public static boolean isDisplayer(ServerLevel level, long position) {
        return get(level).has(level, Positions.toChunk(position), position);
    }

    @Override
    public @NotNull Object2ObjectMap<Identifier, Long2ObjectMap<Set<Long>>> data() {
        return this.data;
    }

    @Override
    public boolean dataTrustable() {
        return false;
    }

    @Override
    public @Nullable Predicate<BlockState> validation() {
        return state -> state.is(NarutoBlocks.DISPLAYER.get());
    }

    public static @NotNull ChunkData<Long, BlockState> get(ServerLevel level) {
        return get(level, Displayers::new, "NarutoDisplayers");
    }

    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.@NotNull Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ChunkPos chunk = event.getChunk().getPos();
            Executor.run(() -> get(level).rebuildChunk(level, chunk));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockChange(@NotNull BlockChangeEvent event) {
        long chunk = event.chunkPos();
        long block = event.blockPos();

        boolean was = event.oldState().is(NarutoBlocks.DISPLAYER.get());
        boolean is = event.newState().is(NarutoBlocks.DISPLAYER.get());

        ServerLevel level = event.level();

        if (was) Executor.run(() -> get(level).remove(level, chunk, block));
        if (is) Executor.run(() -> get(level).add(level, chunk, block));
    }
}
