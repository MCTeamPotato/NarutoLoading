package me.kall.narutoloading.core.detection.inworld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import me.kall.duplicationless.util.Positions;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoInWorldRenderer;
import me.kall.narutoloading.data.saved.Displayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Objects;
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

                    Screen screen = new Screen(new BlockPos(minX, y, minZ), new BlockPos(minX, y + height - 1, minZ), xAxis ? new BlockPos(maxX, y, minZ) : new BlockPos(minX, y, maxZ), xAxis ? new BlockPos(maxX, y + height - 1, minZ) : new BlockPos(minX, y + height - 1, maxZ), dim);

                    NarutoInWorldRenderer.SCREENS.computeIfAbsent(dim, key -> new ObjectOpenHashSet<>()).add(screen);
                    player.displayClientMessage(Component.translatable("info.narutoloading.screen.created", screen.toString()), false);
                } else {
                    lastCorners.put(playerID, corner);
                    player.displayClientMessage(Component.translatable("info.narutoloading.set.first", currentCorner.toShortString()), false);
                }
            }
        }
    }

    public record Screen(BlockPos leftBottomCorner, BlockPos leftTopCorner, BlockPos rightBottomCorner, BlockPos rightTopCorner, ResourceLocation dimension) {
        public void renderImage(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, ResourceLocation textureLocation) {
            RenderType renderType = RenderType.entityTranslucentCull(textureLocation);
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

            BlockPos leftBottomCorner = this.leftBottomCorner();
            BlockPos leftTopCorner = this.leftTopCorner();
            BlockPos rightBottomCorner = this.rightBottomCorner();
            BlockPos rightTopCorner = this.rightTopCorner();

            double leftBottomCornerX = leftBottomCorner.getX();
            double leftBottomCornerY = leftBottomCorner.getY();
            double leftBottomCornerZ = leftBottomCorner.getZ();

            double leftTopCornerX = leftTopCorner.getX();
            double leftTopCornerY = leftTopCorner.getY();
            double leftTopCornerZ = leftTopCorner.getZ();

            double rightBottomCornerX = rightBottomCorner.getX();
            double rightBottomCornerY = rightBottomCorner.getY();
            double rightBottomCornerZ = rightBottomCorner.getZ();

            double rightTopCornerX = rightTopCorner.getX();
            double rightTopCornerY = rightTopCorner.getY();
            double rightTopCornerZ = rightTopCorner.getZ();

            double leftCornerDistX = leftTopCornerX - leftBottomCornerX;
            double leftCornerDistY = leftTopCornerY - leftBottomCornerY;
            double leftCornerDistZ = leftTopCornerZ - leftBottomCornerZ;

            double rightCornerDistX = rightBottomCornerX - leftBottomCornerX;
            double rightCornerDistY = rightBottomCornerY - leftBottomCornerY;
            double rightCornerDistZ = rightBottomCornerZ - leftBottomCornerZ;

            double normalX = leftCornerDistY * rightCornerDistZ - leftCornerDistZ * rightCornerDistY;
            double normalY = leftCornerDistZ * rightCornerDistX - leftCornerDistX * rightCornerDistZ;
            double normalZ = leftCornerDistX * rightCornerDistY - leftCornerDistY * rightCornerDistX;

            double length = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
            normalX /= length;
            normalY /= length;
            normalZ /= length;

            double againstZFighting = 0.01;
            leftBottomCornerX += normalX * againstZFighting;
            leftBottomCornerY += normalY * againstZFighting;
            leftBottomCornerZ += normalZ * againstZFighting;

            leftTopCornerX += normalX * againstZFighting;
            leftTopCornerY += normalY * againstZFighting;
            leftTopCornerZ += normalZ * againstZFighting;

            rightBottomCornerX += normalX * againstZFighting;
            rightBottomCornerY += normalY * againstZFighting;
            rightBottomCornerZ += normalZ * againstZFighting;

            rightTopCornerX += normalX * againstZFighting;
            rightTopCornerY += normalY * againstZFighting;
            rightTopCornerZ += normalZ * againstZFighting;

            Matrix4f pose = poseStack.last().pose();
            Matrix3f normal = poseStack.last().normal();

            vertexConsumer
                    .vertex(pose, (float)leftBottomCornerX, (float)leftBottomCornerY, (float)leftBottomCornerZ)
                    .color(255, 255, 255, 255)
                    .uv(1, 1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(15728880)
                    .normal(normal, (float)normalX, (float)normalY, (float)normalZ)
                    .endVertex();
            vertexConsumer
                    .vertex(pose, (float)leftTopCornerX, (float)leftTopCornerY, (float)leftTopCornerZ)
                    .color(255, 255, 255, 255)
                    .uv(1, 0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(15728880)
                    .normal(normal, (float)normalX, (float)normalY, (float)normalZ)
                    .endVertex();
            vertexConsumer
                    .vertex(pose, (float)rightTopCornerX, (float)rightTopCornerY, (float)rightTopCornerZ)
                    .color(255, 255, 255, 255)
                    .uv(0, 0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(15728880)
                    .normal(normal, (float)normalX, (float)normalY, (float)normalZ)
                    .endVertex();
            vertexConsumer
                    .vertex(pose, (float)rightBottomCornerX, (float)rightBottomCornerY, (float)rightBottomCornerZ)
                    .color(255, 255, 255, 255)
                    .uv(0, 1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(15728880)
                    .normal(normal, (float)normalX, (float)normalY, (float)normalZ)
                    .endVertex();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Screen screen) {
                return screen.leftBottomCorner.equals(this.leftBottomCorner) && screen.leftTopCorner.equals(this.leftTopCorner) && screen.rightTopCorner.equals(this.rightTopCorner) && screen.rightBottomCorner.equals(this.rightBottomCorner) && screen.dimension.equals(this.dimension);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.leftBottomCorner, this.leftTopCorner, this.rightBottomCorner, this.rightTopCorner, this.dimension);
        }

        @Override
        public @NotNull String toString() {
            return Component.translatable("screen.narutoloading.arg", this.leftBottomCorner.toShortString(), this.leftTopCorner.toShortString(), this.rightBottomCorner.toShortString(), this.rightTopCorner.toShortString(), this.dimension.toString()).getString();
        }
    }
}