package me.kall.narutoloading.wall.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("NullableProblems")
public final class WallReloadListener extends SimplePreparableReloadListener<Void> {
    private WallReloadListener() {}

    public static void register(@NotNull RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new WallReloadListener());
    }

    @Override
    protected Void prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(@NotNull Void unused, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        Minecraft.getInstance().execute(WallManager::rebuildActive);
    }
}
