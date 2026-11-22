package me.kall.narutoloading.world;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("NullableProblems")
public final class WallReload extends SimplePreparableReloadListener<Void> {
    private WallReload() {}

    public static void register(@NotNull RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new WallReload());
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
