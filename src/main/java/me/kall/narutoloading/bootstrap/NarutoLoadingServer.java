package me.kall.narutoloading.bootstrap;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.wall.server.WallCommands;
import me.kall.narutoloading.wall.server.WallBuilder;
import me.kall.narutoloading.wall.server.WallInteractions;
import me.kall.narutoloading.wall.server.WallValidation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID)
public final class NarutoLoadingServer {
    private NarutoLoadingServer() {}

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.@NotNull RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isShiftKeyDown()) return;

        if (WallInteractions.interact(player, event.getPos(), event.getItemStack())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.@NotNull PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) WallInteractions.sync(player);
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.@NotNull PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) WallInteractions.sync(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.@NotNull PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) WallInteractions.sync(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.@NotNull PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) WallBuilder.forget(player.getUUID());
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.@NotNull ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        WallValidation.tick(event.getServer());
    }

    @SubscribeEvent
    public static void registerCommands(@NotNull RegisterCommandsEvent event) {
        WallCommands.register(event.getDispatcher());
    }
}
