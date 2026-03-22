package me.kall.narutoloading.inworld.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.duplicationless.data.ChunkData;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.duplicationless.ext.RegistryEntry;
import me.kall.duplicationless.util.Executor;
import me.kall.duplicationless.util.Positions;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.block.Displayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID)
public class Displayers extends ChunkData.BlockData {
    private final Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<Long>>> data = new Object2ObjectOpenHashMap<>();

    public static boolean isDisplayer(ServerLevel level, long position) {
        return get(level).has(level, Positions.toChunk(position), position);
    }

    @Override
    public @NotNull Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<Long>>> data() {
        return this.data;
    }

    @Override
    public boolean dataTrustable() {
        return false;
    }

    @Override
    public @Nullable Predicate<BlockState> validation() {
        return Displayers::isDisplayer;
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

        boolean was = isDisplayer(event.oldState());
        boolean is = isDisplayer(event.newState());

        ServerLevel level = event.level();

        if (was) Executor.run(() -> get(level).remove(level, chunk, block));
        if (is) Executor.run(() -> get(level).add(level, chunk, block));
    }

    public static boolean isDisplayer(@NotNull BlockState state) {
        return Displayer.DISPLAYERS.contains(RegistryEntry.get(state));
    }
}
