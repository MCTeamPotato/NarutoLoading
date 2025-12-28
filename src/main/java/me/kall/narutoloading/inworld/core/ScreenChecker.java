package me.kall.narutoloading.inworld.core;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.duplicationless.util.Executor;
import me.kall.duplicationless.util.Positions;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.data.Screens;
import me.kall.narutoloading.inworld.init.NarutoBlocks;
import me.kall.narutoloading.inworld.network.ScreenDelivery;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID)
public class ScreenChecker {
    private static final Object2ObjectMap<ResourceLocation, Object2LongMap<UUID>> CORNERS = new Object2ObjectOpenHashMap<>();
    private static final Int2IntMap SCREEN_SIZES = new Int2IntOpenHashMap();

    static {
        for (int i = 1; i < 30; i++) {
            SCREEN_SIZES.put(16 * i, 9 * i);
        }
    }

    private static boolean isDisplayer(ServerLevel level, long placement) {
        return Displayers.get(level).has(level, Positions.toChunk(placement), placement);
    }

    public static int dist(@NotNull BlockPos a, @NotNull BlockPos b) {
        if (a.getY() != b.getY()) return -1;

        int dx = Math.abs(a.getX() - b.getX());
        int dz = Math.abs(a.getZ() - b.getZ());

        if (dx != 0 && dz != 0) return -1;
        if (dx == 0 && dz == 0) return 0;

        return dx + dz;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rightClick(PlayerInteractEvent.@NotNull RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level && event.getItemStack().is(Items.STICK)) {
            BlockPos currentCorner = event.getPos();
            long corner = event.getPos().asLong();
            ResourceLocation dim = level.dimension().location();
            UUID playerID = player.getUUID();

            if (isDisplayer(level, corner)) {
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
                    int y = lastCorner.getY();

                    int width = dist(lastCorner, currentCorner);
                    int height = SCREEN_SIZES.getOrDefault(width + 1, -1);

                    if (height == -1) {
                        player.displayClientMessage(Component.translatable("info.narutoloading.screen.invalid_size", String.valueOf(width + 1)), false);
                        return;
                    }

                    boolean xAxis = minX != maxX;
                    if (xAxis && maxX - minX != width) return;
                    if (!xAxis && maxZ - minZ != width) return;

                    BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

                    for (int i = 0; i < width + 1; i++) {
                        if (xAxis) {
                            mutable.set(minX + i, y, minZ);
                        } else {
                            mutable.set(minX, y, minZ + i);
                        }

                        if (!isDisplayer(level, mutable.asLong())) {
                            player.displayClientMessage(Component.translatable("info.narutoloading.screen.displayer_not_found", mutable.toShortString()), false);
                            return;
                        }
                    }

                    int topY = y + height - 1;

                    for (int i = 0; i < width + 1; i++) {
                        if (xAxis) {
                            mutable.set(minX + i, topY, minZ);
                        } else {
                            mutable.set(minX, topY, minZ + i);
                        }

                        if (!isDisplayer(level, mutable.asLong())) {
                            player.displayClientMessage(Component.translatable("info.narutoloading.screen.displayer_not_found", mutable.toShortString()), false);
                            return;
                        }
                    }

                    for (int j = 0; j < height; j++) {
                        mutable.set(minX, y + j, minZ);
                        if (!isDisplayer(level, mutable.asLong())) {
                            player.displayClientMessage(Component.translatable("info.narutoloading.screen.displayer_not_found", mutable.toShortString()), false);
                            return;
                        }

                        if (xAxis) {
                            mutable.set(maxX, y + j, minZ);
                        } else {
                            mutable.set(minX, y + j, maxZ);
                        }

                        if (!isDisplayer(level, mutable.asLong())) {
                            player.displayClientMessage(Component.translatable("info.narutoloading.screen.displayer_not_found", mutable.toShortString()), false);
                            return;
                        }
                    }

                    InWorldScreen inWorldScreen = new InWorldScreen(new BlockPos(minX, y, minZ), new BlockPos(minX, y + height - 1, minZ), xAxis ? new BlockPos(maxX, y, minZ) : new BlockPos(minX, y, maxZ), xAxis ? new BlockPos(maxX, y + height - 1, minZ) : new BlockPos(minX, y + height - 1, maxZ), dim);

                    Screens screenData = Screens.get(level);
                    if (screenData.screens.computeIfAbsent(inWorldScreen.dimension(), key -> new ObjectOpenHashSet<>()).add(inWorldScreen)) screenData.setDirty();

                    ScreenDelivery.INSTANCE.send(PacketDistributor.ALL.noArg(), new ScreenDelivery(inWorldScreen, false));

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
                    if (nextInWorldScreen.involved().contains(block)) {
                        InWorldScreen copy = nextInWorldScreen.copy();

                        screenIterator.remove();
                        screenData.setDirty();

                        Component component = Component.translatable("info.narutoloading.screen.destroy", copy.toLocalString());
                        for (ServerPlayer player : level.players()) {
                            player.displayClientMessage(component, false);
                        }
                        ScreenDelivery.INSTANCE.send(PacketDistributor.ALL.noArg(), new ScreenDelivery(copy, true));
                    }
                }
            });
        }
    }
}