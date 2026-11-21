package me.kall.narutoloading;

import me.kall.narutoloading.client.util.PasteListener;
import me.kall.narutoloading.impl.Substitution;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoLoading.MOD_ID)
public class NarutoLoadingClient {
    @SubscribeEvent
    public static void tickSubstitution(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) Substitution.tick();
    }

    @SubscribeEvent
    public static void tickPasteListener(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.END)) PasteListener.tick();
    }
}
