package me.kall.narutoloading.inworld.data;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class HiddenDisplayers {
    public static final Object2ObjectOpenHashMap<ResourceLocation, Long2IntOpenHashMap> HIDDEN_DISPLAYERS = new Object2ObjectOpenHashMap<>();

    public static boolean isHidden(ResourceLocation dim, BlockPos pos) {
        Long2IntOpenHashMap displayers = HIDDEN_DISPLAYERS.get(dim);
        if (displayers == null) return false;
        return displayers.containsKey(pos.asLong());
    }

    public static void hide(@NotNull InWorldScreen screen) {
        Long2IntOpenHashMap displayers = HIDDEN_DISPLAYERS.computeIfAbsent(screen.dimension(), key -> new Long2IntOpenHashMap());
        LongIterator areaInvolved = screen.areaInvolved().iterator();
        while (areaInvolved.hasNext()) {
            long next = areaInvolved.nextLong();
            displayers.addTo(next, 1);
        }
    }

    public static void reveal(@NotNull InWorldScreen screen) {
        Long2IntOpenHashMap displayers = HIDDEN_DISPLAYERS.get(screen.dimension());
        if (displayers == null) return;
        LongIterator areaInvolved = screen.areaInvolved().iterator();
        while (areaInvolved.hasNext()) {
            long next = areaInvolved.nextLong();
            if (displayers.addTo(next, -1) <= 1) displayers.remove(next);
        }
    }
}
