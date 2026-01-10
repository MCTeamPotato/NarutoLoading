package me.kall.narutoloading.inworld.core;

import it.unimi.dsi.fastutil.objects.*;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.duplicationless.util.Executor;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.data.Screens;
import me.kall.narutoloading.inworld.ext.ScreenLevel;
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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.LongPredicate;

@EventBusSubscriber(modid = NarutoLoading.MOD_ID)
public class ServerScreenChecker {
    private static final Object2ObjectMap<ResourceLocation, Object2LongMap<UUID>> CORNERS = new Object2ObjectOpenHashMap<>();

    private static int dist(@NotNull BlockPos a, @NotNull BlockPos b) {
        return Math.max(Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())), Math.abs(a.getZ() - b.getZ()));
    }

    private static @NotNull InWorldScreen @NotNull [] screenCandidates(@NotNull BlockPos lastCorner, @NotNull BlockPos currentCorner, int height, @NotNull ResourceLocation dimension) {
        InWorldScreen[] screenCandidates = new InWorldScreen[]{null, null, null, null};

        int dx = Integer.compare(currentCorner.getX(), lastCorner.getX());
        int dy = Integer.compare(currentCorner.getY(), lastCorner.getY());
        int dz = Integer.compare(currentCorner.getZ(), lastCorner.getZ());

        boolean widthX = dx != 0;
        boolean widthY = dy != 0;
        boolean widthZ = dz != 0;

        if (widthX) {
            screenCandidates[0] = build(lastCorner, currentCorner, 0, height, 0, dimension);
            screenCandidates[1] = build(lastCorner, currentCorner, 0, -height, 0, dimension);
            screenCandidates[2] = build(lastCorner, currentCorner, 0, 0, height, dimension);
            screenCandidates[3] = build(lastCorner, currentCorner, 0, 0, -height, dimension);
        } else if (widthY) {
            screenCandidates[0] = build(lastCorner, currentCorner, height, 0, 0, dimension);
            screenCandidates[1] = build(lastCorner, currentCorner, -height, 0, 0, dimension);
            screenCandidates[2] = build(lastCorner, currentCorner, 0, 0, height, dimension);
            screenCandidates[3] = build(lastCorner, currentCorner, 0, 0, -height, dimension);
        } else if (widthZ) {
            screenCandidates[0] = build(lastCorner, currentCorner, height, 0, 0, dimension);
            screenCandidates[1] = build(lastCorner, currentCorner, -height, 0, 0, dimension);
            screenCandidates[2] = build(lastCorner, currentCorner, 0, height, 0, dimension);
            screenCandidates[3] = build(lastCorner, currentCorner, 0, -height, 0, dimension);
        }

        return screenCandidates;
    }

    @Contract("_, _, _, _, _, _ -> new")
    private static @NotNull InWorldScreen build(@NotNull BlockPos lastCorner, @NotNull BlockPos currentCorner, int hx, int hy, int hz, ResourceLocation dimension) {
        return new InWorldScreen(lastCorner, lastCorner.offset(hx, hy, hz), currentCorner, currentCorner.offset(hx, hy, hz), dimension);
    }

    private static int @NotNull [] forHeights(int width) {
        int[] heights = new int[]{0, 0};
        if (width >= 16 && width % 16 == 0) heights[0] = (width * 9) / 16 - 1;
        if (width >= 9 && width % 9 == 0) heights[1] = (width * 16) / 9 - 1;
        return heights;
    }

    private static @Nullable InWorldScreen validate(InWorldScreen @NotNull [] screens, @NotNull LongPredicate predicate) {
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

                    int width = ServerScreenChecker.dist(lastCorner, currentCorner);
                    int[] heights = ServerScreenChecker.forHeights(width + 1);

                    if (heights[0] == 0 && heights[1] == 0) {
                        player.displayClientMessage(Component.translatable("info.narutoloading.screen.invalid_size", String.valueOf(width + 1)), false);
                        return;
                    }

                    boolean xAxis = minX != maxX;
                    if (xAxis && maxX - minX != width) return;
                    if (!xAxis && maxZ - minZ != width) return;

                    InWorldScreen inWorldScreen = null;
                    for (int height : heights) {
                        inWorldScreen = ServerScreenChecker.validate(ServerScreenChecker.screenCandidates(lastCorner, currentCorner, height, dim), posLong -> Displayers.isDisplayer(level, posLong));
                        if (inWorldScreen != null) break;
                    }

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
                if (((ScreenLevel)level).naruto$isClearingScreens()) return;

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
                        for (ServerPlayer player : level.players()) player.displayClientMessage(component, false);
                        PacketDistributor.sendToAllPlayers(new ScreenLifePacket(copy, true));
                    }
                }
            });
        }
    }
}