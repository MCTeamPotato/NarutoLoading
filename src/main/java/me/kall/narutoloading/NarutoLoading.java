package me.kall.narutoloading;

import me.kall.narutoloading.bootstrap.ClientSetup;
import me.kall.narutoloading.bootstrap.Integration;
import me.kall.narutoloading.network.NarutoNetwork;
import me.kall.narutoloading.wall.server.WallBlockChanges;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";

    public NarutoLoading(IEventBus modBus, @NotNull Dist dist, ModContainer modContainer) {
        modBus.addListener(NarutoNetwork::register);
        if (Integration.DUPLICATIONLESS) NeoForge.EVENT_BUS.register(new WallBlockChanges());
        if (dist.isClient()) ClientSetup.register(modContainer);
    }
}
