package me.kall.narutoloading.inworld.core;

import it.unimi.dsi.fastutil.objects.*;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.duplicationless.util.Executor;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.data.Screens;
import me.kall.narutoloading.inworld.init.NarutoBlocks;
import me.kall.narutoloading.inworld.network.ScreenLifePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.LongPredicate;

@EventBusSubscriber(modid = NarutoLoading.MOD_ID)
public class ServerScreenChecker {
    private static final Object2ObjectMap<ResourceLocation, Object2LongMap<UUID>> CORNERS = new Object2ObjectOpenHashMap<>();

    public static int dist(@NotNull BlockPos a, @NotNull BlockPos b) {
        return Math.max(Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())), Math.abs(a.getZ() - b.getZ()));
    }

    public static @NotNull List<InWorldScreen> screenCandidates(@NotNull BlockPos lastCorner, @NotNull BlockPos currentCorner, int height, @NotNull ResourceLocation dimension) {
        List<InWorldScreen> result = new ObjectArrayList<>(4);

        int dx = Integer.compare(currentCorner.getX(), lastCorner.getX());
        int dy = Integer.compare(currentCorner.getY(), lastCorner.getY());
        int dz = Integer.compare(currentCorner.getZ(), lastCorner.getZ());

        boolean widthX = dx != 0;
        boolean widthY = dy != 0;
        boolean widthZ = dz != 0;

        if (widthX) {
            tryAddScreens(lastCorner, currentCorner, 0, height, 0, dimension, result);
            tryAddScreens(lastCorner, currentCorner, 0, -height, 0, dimension, result);
            tryAddScreens(lastCorner, currentCorner, 0, 0, height, dimension, result);
            tryAddScreens(lastCorner, currentCorner, 0, 0, -height, dimension, result);
        } else if (widthY) {
            tryAddScreens(lastCorner, currentCorner, height, 0, 0, dimension, result);
            tryAddScreens(lastCorner, currentCorner, -height, 0, 0, dimension, result);
            tryAddScreens(lastCorner, currentCorner, 0, 0, height, dimension, result);
            tryAddScreens(lastCorner, currentCorner, 0, 0, -height, dimension, result);
        } else if (widthZ) {
            tryAddScreens(lastCorner, currentCorner, height, 0, 0, dimension, result);
            tryAddScreens(lastCorner, currentCorner, -height, 0, 0, dimension, result);
            tryAddScreens(lastCorner, currentCorner, 0, height, 0, dimension, result);
            tryAddScreens(lastCorner, currentCorner, 0, -height, 0, dimension, result);
        }

        return result;
    }

    private static void tryAddScreens(@NotNull BlockPos lastCorner, @NotNull BlockPos currentCorner, int hx, int hy, int hz, ResourceLocation dimension, @NotNull List<InWorldScreen> result) {
        result.add(new InWorldScreen(lastCorner, lastCorner.offset(hx, hy, hz), currentCorner, currentCorner.offset(hx, hy, hz), dimension));
    }

    private static int forHeight(int width) {
        if (width % 16 != 0 || width < 16) {
            return -1;
        }
        return (width * 9) / 16 - 1;
    }

    public static @Nullable InWorldScreen validate(@NotNull List<InWorldScreen> screens, @NotNull LongPredicate predicate) {
        InWorldScreen result = null;
        for (InWorldScreen screen : screens) {
            boolean valid = true;
            for (long pos : screen.borderInvolved()) {
                if (!predicate.test(pos)) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                result = screen;
                break;
            }
        }

        return result;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rightClick(PlayerInteractEvent.@NotNull RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level && player.isShiftKeyDown() && event.getItemStack().is(Items.STICK)) {
            BlockPos currentCorner = event.getPos();
            long corner = event.getPos().asLong();
            ResourceLocation dim = level.dimension().location();
            UUID playerID = player.getUUID();

            if (Displayers.isDisplayer(level, corner)) {
                Object2LongMap<UUID> lastCorners = CORNERS.computeIfAbsent(dim, key -> new Object2LongOpenHashMap<>());

                if (lastCorners.containsKey(playerID)) {
                    BlockPos lastCorner = BlockPos.of(lastCorners.getLong(playerID));
                    lastCorners.removeLong(playerID);
                    player.displayClientMessage(Component.translatable("info.narutoloading.set.second", currentCorner.toShortString()), false);
                    player.displayClientMessage(Component.translatable("info.narutoloading.screen"), false);

                    int minX = Math.min(lastCorner.getX(), currentCorner.getX());
                    int maxX = Math.max(lastCorner.getX(), currentCorner.getX());
                    int minZ = Math.min(lastCorner.getZ(), currentCorner.getZ());
                    int maxZ = Math.max(lastCorner.getZ(), currentCorner.getZ());

                    int width = dist(lastCorner, currentCorner);
                    int height = forHeight(width + 1);

                    if (height == -1) {
                        player.displayClientMessage(Component.translatable("info.narutoloading.screen.invalid_size", String.valueOf(width + 1)), false);
                        return;
                    }

                    boolean xAxis = minX != maxX;
                    if (xAxis && maxX - minX != width) return;
                    if (!xAxis && maxZ - minZ != width) return;

                    InWorldScreen inWorldScreen = validate(screenCandidates(lastCorner, currentCorner, height, dim), posLong -> Displayers.isDisplayer(level, posLong));

                    if (inWorldScreen == null) {
                        player.displayClientMessage(Component.translatable("info.narutoloading.screen.fail"), false);
                        return;
                    }

                    Screens screenData = Screens.get(level);
                    if (screenData.screens.computeIfAbsent(inWorldScreen.dimension(), key -> new ObjectOpenHashSet<>()).add(inWorldScreen)) {
                        screenData.setDirty();
                    }

                    PacketDistributor.sendToAllPlayers(new ScreenLifePacket(inWorldScreen, false));

                    player.displayClientMessage(Component.translatable("info.narutoloading.screen.created", inWorldScreen.toLocalString()), false);
                } else {
                    lastCorners.put(playerID, corner);
                    player.displayClientMessage(Component.translatable("info.narutoloading.set.first", currentCorner.toShortString()), false);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockChange(@NotNull BlockChangeEvent event) {
        ServerLevel level = event.level();
        ResourceLocation dimension = level.dimension().location();
        long block = event.blockPos();
        if (event.oldState().is(NarutoBlocks.DISPLAYER.get())) {
            Executor.run(() -> {
                Screens screenData = Screens.get(level);
                ObjectSet<InWorldScreen> inWorldScreens = screenData.screens.get(dimension);
                if (inWorldScreens == null || inWorldScreens.isEmpty()) return;

                ObjectIterator<InWorldScreen> screenIterator = inWorldScreens.iterator();
                while (screenIterator.hasNext()) {
                    InWorldScreen nextInWorldScreen = screenIterator.next();
                    if (nextInWorldScreen.borderInvolved().contains(block)) {
                        InWorldScreen copy = nextInWorldScreen.finalCopy();

                        screenIterator.remove();
                        screenData.setDirty();

                        Component component = Component.translatable("info.narutoloading.screen.destroy", copy.toLocalString());
                        for (ServerPlayer player : level.players()) {
                            player.displayClientMessage(component, false);
                        }
                        PacketDistributor.sendToAllPlayers(new ScreenLifePacket(copy, true));
                    }
                }
            });
        }
    }
}