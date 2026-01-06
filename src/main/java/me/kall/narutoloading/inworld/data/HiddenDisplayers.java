package me.kall.narutoloading.inworld.data;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
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
            displayers.addTo(areaInvolved.nextLong(), 1);
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

        if (displayers.isEmpty()) HIDDEN_DISPLAYERS.remove(screen.dimension());
    }

    @SubscribeEvent
    public static void logOutClean(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft.getInstance().execute(HiddenDisplayers.HIDDEN_DISPLAYERS::clear);
    }
}
