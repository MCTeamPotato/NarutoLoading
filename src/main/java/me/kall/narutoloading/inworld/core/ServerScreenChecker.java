package me.kall.narutoloading.inworld.core;

import it.unimi.dsi.fastutil.objects.*;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.duplicationless.util.Executor;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.data.Screens;
import me.kall.narutoloading.inworld.init.NarutoBlocks;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import me.kall.narutoloading.inworld.network.ScreenLifePacket;
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
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID)
public class ServerScreenChecker {
    private static final Object2ObjectMap<ResourceLocation, Object2LongMap<UUID>> CORNERS = new Object2ObjectOpenHashMap<>();

    public static int dist(@NotNull BlockPos a, @NotNull BlockPos b) {
        if (a.getY() != b.getY()) return -1;

        int dx = Math.abs(a.getX() - b.getX());
        int dz = Math.abs(a.getZ() - b.getZ());

        if (dx != 0 && dz != 0) return -1;
        if (dx == 0 && dz == 0) return 0;

        return dx + dz;
    }

    public static @Nullable BlockPos checkBorder(int minX, int y, int minZ, int maxX, int maxZ, int width, int height, boolean isXAxis, @NotNull Predicate<BlockPos.MutableBlockPos> predicate) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int i = 0; i < width + 1; i++) {
            if (isXAxis) {
                mutable.set(minX + i, y, minZ);
            } else {
                mutable.set(minX, y, minZ + i);
            }

            if (!predicate.test(mutable)) {
                return mutable.immutable();
            }
        }

        int topY = y + height - 1;

        for (int i = 0; i < width + 1; i++) {
            if (isXAxis) {
                mutable.set(minX + i, topY, minZ);
            } else {
                mutable.set(minX, topY, minZ + i);
            }

            if (!predicate.test(mutable)) {
                return mutable.immutable();
            }
        }

        for (int j = 0; j < height; j++) {
            mutable.set(minX, y + j, minZ);

            if (!predicate.test(mutable)) {
                return mutable.immutable();
            }

            if (isXAxis) {
                mutable.set(maxX, y + j, minZ);
            } else {
                mutable.set(minX, y + j, maxZ);
            }

            if (!predicate.test(mutable)) {
                return mutable.immutable();
            }
        }

        return null;
    }

    private static int forHeight(int width) {
        if (width % 16 != 0 || width < 16 || width >= 16 * 4096) {
            return -1;
        }
        return (width * 9) / 16;
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
                    int y = lastCorner.getY();

                    int width = dist(lastCorner, currentCorner);
                    int height = forHeight(width + 1);

                    if (height == -1) {
                        player.displayClientMessage(Component.translatable("info.narutoloading.screen.invalid_size", String.valueOf(width + 1)), false);
                        return;
                    }

                    boolean xAxis = minX != maxX;
                    if (xAxis && maxX - minX != width) return;
                    if (!xAxis && maxZ - minZ != width) return;

                    BlockPos failedPos = checkBorder(minX, y, minZ, maxX, maxZ, width, height, xAxis, mutable -> Displayers.isDisplayer(level, mutable.asLong()));

                    if (failedPos != null) {
                        player.displayClientMessage(Component.translatable("info.narutoloading.screen.displayer_not_found", failedPos.toShortString()), false);
                        return;
                    }

                    InWorldScreen inWorldScreen = new InWorldScreen(new BlockPos(minX, y, minZ), new BlockPos(minX, y + height - 1, minZ), xAxis ? new BlockPos(maxX, y, minZ) : new BlockPos(minX, y, maxZ), xAxis ? new BlockPos(maxX, y + height - 1, minZ) : new BlockPos(minX, y + height - 1, maxZ), dim);

                    Screens screenData = Screens.get(level);
                    if (screenData.screens.computeIfAbsent(inWorldScreen.dimension(), key -> new ObjectOpenHashSet<>()).add(inWorldScreen)) {
                        screenData.setDirty();
                    }

                    NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new ScreenLifePacket(inWorldScreen, false));

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
                        NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new ScreenLifePacket(copy, true));
                    }
                }
            });
        }
    }
}