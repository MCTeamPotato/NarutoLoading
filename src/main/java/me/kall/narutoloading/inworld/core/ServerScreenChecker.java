package me.kall.narutoloading.inworld.core;

import it.unimi.dsi.fastutil.objects.*;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.duplicationless.util.Executor;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.data.Screens;
import me.kall.narutoloading.inworld.ext.ScreenLevel;
import me.kall.narutoloading.inworld.init.NarutoBlocks;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import me.kall.narutoloading.inworld.network.ScreenLifePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongPredicate;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID)
public class ServerScreenChecker {
    private static final Object2ObjectMap<ResourceLocation, Object2LongMap<UUID>> BLOCK_CORNERS = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectMap<ResourceLocation, Map<UUID, EntityCorner>> ENTITY_CORNERS = new Object2ObjectOpenHashMap<>();

    private record EntityCorner(long wallBlockPos, Direction facing) {}

    private static int dist(@NotNull BlockPos a, @NotNull BlockPos b) {
        return Math.max(Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())), Math.abs(a.getZ() - b.getZ()));
    }

    private static @NotNull InWorldScreen @NotNull [] screenCandidates(@NotNull BlockPos lastCorner, @NotNull BlockPos currentCorner, int height, @NotNull ResourceLocation dimension) {
        InWorldScreen[] screenCandidates = new InWorldScreen[]{null, null, null, null};

        boolean widthX = currentCorner.getX() != lastCorner.getX();
        boolean widthY = currentCorner.getY() != lastCorner.getY();
        boolean widthZ = currentCorner.getZ() != lastCorner.getZ();

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

    private static @Nullable InWorldScreen validate(InWorldScreen @NotNull [] screens, @NotNull LongPredicate predicate) {
        for (InWorldScreen screen : screens) {
            if (screen == null) continue;
            boolean valid = true;
            for (long pos : screen.borderInvolved()) {
                if (!predicate.test(pos)) {
                    valid = false;
                    break;
                }
            }
            if (valid) return screen;
        }
        return null;
    }

    private static boolean isHangingEntityAt(@NotNull ServerLevel level, long entityPosLong) {
        BlockPos pos = BlockPos.of(entityPosLong);
        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        return !level.getEntitiesOfClass(ItemFrame.class, box).isEmpty();
    }

    private static void setHangingEntitiesInvisible(@NotNull ServerLevel level, @NotNull InWorldScreen screen, @NotNull Direction facing, boolean invisible) {
        for (long wallPosLong : screen.borderInvolved()) {
            BlockPos entityPos = BlockPos.of(wallPosLong).relative(facing);
            AABB box = new AABB(entityPos.getX(), entityPos.getY(), entityPos.getZ(), entityPos.getX() + 1, entityPos.getY() + 1, entityPos.getZ() + 1);
            for (ItemFrame entity : level.getEntitiesOfClass(ItemFrame.class, box)) {
                entity.setInvisible(invisible);
            }
        }
    }

    private static @Nullable InWorldScreen tryBuildScreen(@NotNull ServerPlayer player, @NotNull ServerLevel level, @NotNull BlockPos lastCorner, @NotNull BlockPos currentCorner, @NotNull LongPredicate borderPredicate) {
        player.displayClientMessage(Component.translatable("info.narutoloading.set.second", currentCorner.toShortString()), false);
        player.displayClientMessage(Component.translatable("info.narutoloading.screen"), false);

        int minX = Math.min(lastCorner.getX(), currentCorner.getX());
        int maxX = Math.max(lastCorner.getX(), currentCorner.getX());
        int minZ = Math.min(lastCorner.getZ(), currentCorner.getZ());
        int maxZ = Math.max(lastCorner.getZ(), currentCorner.getZ());

        int width = ServerScreenChecker.dist(lastCorner, currentCorner);

        boolean xAxis = minX != maxX;
        if (xAxis && maxX - minX != width) return null;
        if (!xAxis && maxZ - minZ != width) return null;

        ResourceLocation dim = level.dimension().location();
        InWorldScreen inWorldScreen = null;
        for (int height = level.getMaxBuildHeight(); height >= 1 && inWorldScreen == null; height--) {
            inWorldScreen = ServerScreenChecker.validate(ServerScreenChecker.screenCandidates(lastCorner, currentCorner, height, dim), borderPredicate);
        }

        if (inWorldScreen == null) {
            player.displayClientMessage(Component.translatable("info.narutoloading.screen.fail"), false);
            return null;
        }

        Screens screenData = Screens.get(level);
        if (screenData.screens.computeIfAbsent(inWorldScreen.dimension(), key -> new ObjectOpenHashSet<>()).add(inWorldScreen)) {
            screenData.setDirty();
        }

        NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new ScreenLifePacket(inWorldScreen, false));
        player.displayClientMessage(Component.translatable("info.narutoloading.screen.created", inWorldScreen.toLocalString()), false);
        return inWorldScreen;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rightClickBlock(PlayerInteractEvent.@NotNull RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!player.isShiftKeyDown()) return;
        if (!event.getItemStack().is(Items.STICK)) return;

        BlockPos currentCorner = event.getPos();
        long corner = currentCorner.asLong();
        ResourceLocation dim = level.dimension().location();
        UUID playerID = player.getUUID();

        if (!Displayers.isDisplayer(level, corner)) return;

        Object2LongMap<UUID> lastCorners = BLOCK_CORNERS.computeIfAbsent(dim, key -> new Object2LongOpenHashMap<>());

        if (lastCorners.containsKey(playerID)) {
            BlockPos lastCorner = BlockPos.of(lastCorners.getLong(playerID));
            lastCorners.removeLong(playerID);
            tryBuildScreen(player, level, lastCorner, currentCorner, posLong -> Displayers.isDisplayer(level, posLong));
        } else {
            lastCorners.put(playerID, corner);
            player.displayClientMessage(Component.translatable("info.narutoloading.set.first", currentCorner.toShortString()), false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rightClickEntity(PlayerInteractEvent.@NotNull EntityInteract event) {
        if (!(event.getTarget() instanceof ItemFrame hanging)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!player.isShiftKeyDown()) return;
        if (!player.getMainHandItem().is(Items.STICK)) return;

        event.setCanceled(true);

        Direction facing = hanging.getDirection();
        BlockPos wallCorner = hanging.blockPosition().relative(facing.getOpposite());
        ResourceLocation dim = level.dimension().location();
        UUID playerID = player.getUUID();

        Map<UUID, EntityCorner> lastCorners = ENTITY_CORNERS.computeIfAbsent(dim, key -> new HashMap<>());

        if (lastCorners.containsKey(playerID)) {
            EntityCorner first = lastCorners.remove(playerID);

            if (first.facing() != facing) {
                player.displayClientMessage(Component.translatable("info.narutoloading.screen.fail"), false);
                return;
            }

            BlockPos firstWallCorner = BlockPos.of(first.wallBlockPos());

            LongPredicate borderPredicate = posLong -> isHangingEntityAt(level, BlockPos.of(posLong).relative(facing).asLong());

            InWorldScreen built = tryBuildScreen(player, level, firstWallCorner, wallCorner, borderPredicate);
            if (built != null) setHangingEntitiesInvisible(level, built, facing, true);
        } else {
            lastCorners.put(playerID, new EntityCorner(wallCorner.asLong(), facing));
            player.displayClientMessage(Component.translatable("info.narutoloading.set.first", wallCorner.toShortString()), false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void entityLeave(@NotNull EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof ItemFrame hanging)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        Entity.RemovalReason reason = hanging.getRemovalReason();
        if (reason == null || !reason.shouldDestroy()) return;

        Direction facing = hanging.getDirection();
        long wallBlock = hanging.blockPosition().relative(facing.getOpposite()).asLong();
        ResourceLocation dimension = level.dimension().location();

        Executor.run(() -> {
            Screens screenData = Screens.get(level);
            ObjectSet<InWorldScreen> inWorldScreens = screenData.screens.get(dimension);
            if (inWorldScreens == null || inWorldScreens.isEmpty()) return;

            ObjectIterator<InWorldScreen> screenIterator = inWorldScreens.iterator();
            while (screenIterator.hasNext()) {
                InWorldScreen nextInWorldScreen = screenIterator.next();
                if (nextInWorldScreen.borderInvolved().contains(wallBlock)) {
                    InWorldScreen copy = nextInWorldScreen.finalCopy();

                    screenIterator.remove();
                    screenData.setDirty();

                    setHangingEntitiesInvisible(level, copy, facing, false);

                    Component component = Component.translatable("info.narutoloading.screen.destroy", copy.toLocalString());
                    for (ServerPlayer player : level.players()) player.displayClientMessage(component, false);
                    NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new ScreenLifePacket(copy, true));
                }
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockChange(@NotNull BlockChangeEvent event) {
        ServerLevel level = event.level();
        ResourceLocation dimension = level.dimension().location();
        long block = event.blockPos();
        if (event.oldState().is(NarutoBlocks.DISPLAYER.get())) {
            Executor.run(() -> {
                if (((ScreenLevel) level).naruto$isClearingScreens()) return;

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
                        NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new ScreenLifePacket(copy, true));
                    }
                }
            });
        }
    }
}