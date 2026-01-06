package me.kall.narutoloading.inworld.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.ext.IFrustum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public class ClientScreensRenderer {
    public static final Object2ObjectMap<ResourceLocation, ObjectSet<NarutoInWorldRenderer>> CLIENT_SCREENS = new Object2ObjectOpenHashMap<>();

    @SubscribeEvent
    public static void logOutClean(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft.getInstance().execute(() -> {
            for (ObjectSet<NarutoInWorldRenderer> renderers : CLIENT_SCREENS.values()) {
                for (NarutoInWorldRenderer renderer : renderers) {
                    renderer.shutdown();
                }
            }
            CLIENT_SCREENS.clear();
            HIDDEN_DISPLAYERS.clear();
        });
    }

    @SubscribeEvent
    public static void renderTick(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (CLIENT_SCREENS.isEmpty()) return;
            for (ObjectSet<NarutoInWorldRenderer> renderers : CLIENT_SCREENS.values()) {
                for (NarutoInWorldRenderer renderer : renderers) {
                    if (renderer.pause) continue;
                    renderer.renderFrame(null);
                }
            }
        }
    }

    @SubscribeEvent
    public static void renderLevel(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        ResourceLocation dimension = level.dimension().location();
        ObjectSet<NarutoInWorldRenderer> renderers = CLIENT_SCREENS.get(dimension);
        if (renderers == null || renderers.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        Frustum frustum = minecraft.levelRenderer.getFrustum();

        for (NarutoInWorldRenderer renderer : renderers) {
            if (!IFrustum.isVisible(frustum, renderer.screen)) continue;
            poseStack.pushPose();
            poseStack.translate(-camera.x, - camera.y, - camera.z);

            renderScreen(poseStack, bufferSource, renderer, frustum, camera);

            poseStack.popPose();
        }
    }

    private static void renderScreen(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, @NotNull NarutoInWorldRenderer renderer, Frustum frustum, Vec3 camera) {
        InWorldScreen inWorldScreen = renderer.screen;
        ResourceLocation nextFrame = renderer.nextFrame();
        if (nextFrame == null) return;

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(nextFrame));

        BlockPos leftBottomCorner = inWorldScreen.leftBottomCorner();
        BlockPos leftTopCorner = inWorldScreen.leftTopCorner();
        BlockPos rightBottomCorner = inWorldScreen.rightBottomCorner();
        BlockPos rightTopCorner = inWorldScreen.rightTopCorner();

        double leftBottomCornerX = leftBottomCorner.getX();
        double leftBottomCornerY = leftBottomCorner.getY();
        double leftBottomCornerZ = leftBottomCorner.getZ();

        double leftTopCornerX = leftTopCorner.getX();
        double leftTopCornerY = leftTopCorner.getY() + 1.0;
        double leftTopCornerZ = leftTopCorner.getZ();

        double rightBottomCornerX = rightBottomCorner.getX();
        double rightBottomCornerY = rightBottomCorner.getY();
        double rightBottomCornerZ = rightBottomCorner.getZ();

        double rightTopCornerX = rightTopCorner.getX();
        double rightTopCornerY = rightTopCorner.getY() + 1.0;
        double rightTopCornerZ = rightTopCorner.getZ();

        boolean isXAxis = leftBottomCornerX != rightBottomCornerX;

        if (isXAxis) {
            rightBottomCornerX += 1.0;
            rightTopCornerX += 1.0;
        } else {
            rightBottomCornerZ += 1.0;
            rightTopCornerZ += 1.0;
        }

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

        double centerX = (leftBottomCornerX + rightTopCornerX) / 2.0;
        double centerY = (leftBottomCornerY + rightTopCornerY) / 2.0;
        double centerZ = (leftBottomCornerZ + rightTopCornerZ) / 2.0;

        double toCameraX = camera.x - centerX;
        double toCameraY = camera.y - centerY;
        double toCameraZ = camera.z - centerZ;

        double dot = normalX * toCameraX + normalY * toCameraY + normalZ * toCameraZ;

        double offsetDirection = dot > 0 ? 1.0 : -1.0;

        boolean isFrontFacing = dot > 0;

        if (dot < 0) {
            normalX = -normalX;
            normalY = -normalY;
            normalZ = -normalZ;
        }

        double distCenter = distanceSquared(camera.x, camera.y, camera.z, centerX, centerY, centerZ);
        double distLeftBottom = distanceSquared(camera.x, camera.y, camera.z, leftBottomCornerX, leftBottomCornerY, leftBottomCornerZ);
        double distLeftTop = distanceSquared(camera.x, camera.y, camera.z, leftTopCornerX, leftTopCornerY, leftTopCornerZ);
        double distRightBottom = distanceSquared(camera.x, camera.y, camera.z, rightBottomCornerX, rightBottomCornerY, rightBottomCornerZ);
        double distRightTop = distanceSquared(camera.x, camera.y, camera.z, rightTopCornerX, rightTopCornerY, rightTopCornerZ);

        double minDistSquared = Math.min(distCenter, Math.min(Math.min(distLeftBottom, distLeftTop), Math.min(distRightBottom, distRightTop)));

        double againstZFighting = (0.01 + Math.sqrt(minDistSquared) * 0.005) * offsetDirection;

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

        if (isFrontFacing) {
            vertexConsumer.vertex(pose, (float)leftBottomCornerX,   (float)leftBottomCornerY,  (float)leftBottomCornerZ).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
            vertexConsumer.vertex(pose, (float)leftTopCornerX,         (float)leftTopCornerY,     (float)leftTopCornerZ).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
            vertexConsumer.vertex(pose, (float)rightTopCornerX,       (float)rightTopCornerY,    (float)rightTopCornerZ).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
            vertexConsumer.vertex(pose, (float)rightBottomCornerX, (float)rightBottomCornerY, (float)rightBottomCornerZ).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
        } else {
            vertexConsumer.vertex(pose, (float)leftBottomCornerX,   (float)leftBottomCornerY,  (float)leftBottomCornerZ).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
            vertexConsumer.vertex(pose, (float)leftTopCornerX,         (float)leftTopCornerY,     (float)leftTopCornerZ).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
            vertexConsumer.vertex(pose, (float)rightTopCornerX,       (float)rightTopCornerY,    (float)rightTopCornerZ).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
            vertexConsumer.vertex(pose, (float)rightBottomCornerX, (float)rightBottomCornerY, (float)rightBottomCornerZ).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
        }
    }

    private static double distanceSquared(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }
}
