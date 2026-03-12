package me.kall.narutoloading.inworld.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public class ClientScreensRenderer {
    public static final Object2ObjectMap<Identifier, ObjectSet<NarutoInWorldRenderer>> CLIENT_SCREENS = new Object2ObjectOpenHashMap<>();

    public static void reload() {
        Minecraft.getInstance().execute(() -> {
            for (ObjectSet<NarutoInWorldRenderer> renderers : ClientScreensRenderer.CLIENT_SCREENS.values()) {
                for (NarutoInWorldRenderer renderer : renderers) {
                    renderer.pause = true;
                    renderer.shutdown();
                    renderer.setup();
                    renderer.pause = false;
                }
            }
        });
    }

    @SubscribeEvent
    public static void logOutClean(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft.getInstance().execute(() -> {
            for (ObjectSet<NarutoInWorldRenderer> renderers : CLIENT_SCREENS.values()) {
                for (NarutoInWorldRenderer renderer : renderers) {
                    renderer.shutdown();
                }
            }
            CLIENT_SCREENS.clear();
        });
    }

    @SubscribeEvent
    public static void renderTick(RenderFrameEvent.Pre event) {
        if (CLIENT_SCREENS.isEmpty()) return;
        for (ObjectSet<NarutoInWorldRenderer> renderers : CLIENT_SCREENS.values()) {
            for (NarutoInWorldRenderer renderer : renderers) {
                if (renderer.pause) continue;
                renderer.renderFrame(null);
            }
        }
    }

    @SubscribeEvent
    public static void renderLevel(@NotNull RenderLevelStageEvent.AfterOpaqueBlocks event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        Identifier dimension = level.dimension().identifier();
        ObjectSet<NarutoInWorldRenderer> renderers = CLIENT_SCREENS.get(dimension);
        if (renderers == null || renderers.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().position();

        for (NarutoInWorldRenderer renderer : renderers) {
            poseStack.pushPose();
            poseStack.translate(-camera.x, - camera.y, - camera.z);
            renderScreen(poseStack, bufferSource, renderer, camera);
            poseStack.popPose();
        }
    }

    private static void renderScreen(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, @NotNull NarutoInWorldRenderer renderer, Vec3 camera) {
        InWorldScreen inWorldScreen = renderer.screen;
        Identifier nextFrame = renderer.nextFrame();
        if (nextFrame == null) return;

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.entityTranslucent(nextFrame));

        BlockPos leftBottomCorner = inWorldScreen.leftBottomCorner();
        BlockPos leftTopCorner = inWorldScreen.leftTopCorner();
        BlockPos rightBottomCorner = inWorldScreen.rightBottomCorner();
        BlockPos rightTopCorner = inWorldScreen.rightTopCorner();

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

        boolean widthX = leftBottomCornerX != rightBottomCornerX;
        boolean widthY = leftBottomCornerY != rightBottomCornerY;
        boolean widthZ = leftBottomCornerZ != rightBottomCornerZ;

        boolean heightX = leftBottomCornerX != leftTopCornerX;
        boolean heightY = leftBottomCornerY != leftTopCornerY;
        boolean heightZ = leftBottomCornerZ != leftTopCornerZ;

        if (widthX && heightY) {
            if (leftTopCornerY > leftBottomCornerY) {
                leftTopCornerY += 1.0;
                rightTopCornerY += 1.0;
            } else {
                leftBottomCornerY += 1.0;
                rightBottomCornerY += 1.0;
            }

            if (rightBottomCornerX > leftBottomCornerX) {
                rightBottomCornerX += 1.0;
                rightTopCornerX += 1.0;
            } else {
                leftBottomCornerX += 1.0;
                leftTopCornerX += 1.0;
            }
        } else if (widthZ && heightY) {
            if (leftTopCornerY > leftBottomCornerY) {
                leftTopCornerY += 1.0;
                rightTopCornerY += 1.0;
            } else {
                leftBottomCornerY += 1.0;
                rightBottomCornerY += 1.0;
            }

            if (rightBottomCornerZ > leftBottomCornerZ) {
                rightBottomCornerZ += 1.0;
                rightTopCornerZ += 1.0;
            } else {
                leftBottomCornerZ += 1.0;
                leftTopCornerZ += 1.0;
            }
        } else if (widthX && heightZ) {
            if (leftTopCornerZ > leftBottomCornerZ) {
                leftTopCornerZ += 1.0;
                rightTopCornerZ += 1.0;
            } else {
                leftBottomCornerZ += 1.0;
                rightBottomCornerZ += 1.0;
            }

            if (rightBottomCornerX > leftBottomCornerX) {
                rightBottomCornerX += 1.0;
                rightTopCornerX += 1.0;
            } else {
                leftBottomCornerX += 1.0;
                leftTopCornerX += 1.0;
            }
        } else if (widthY && heightX) {
            if (leftTopCornerX > leftBottomCornerX) {
                leftTopCornerX += 1.0;
                rightTopCornerX += 1.0;
            } else {
                leftBottomCornerX += 1.0;
                rightBottomCornerX += 1.0;
            }

            if (rightBottomCornerY > leftBottomCornerY) {
                rightBottomCornerY += 1.0;
                rightTopCornerY += 1.0;
            } else {
                leftBottomCornerY += 1.0;
                leftTopCornerY += 1.0;
            }
        } else if (widthY && heightZ) {
            if (leftTopCornerZ > leftBottomCornerZ) {
                leftTopCornerZ += 1.0;
                rightTopCornerZ += 1.0;
            } else {
                leftBottomCornerZ += 1.0;
                rightBottomCornerZ += 1.0;
            }

            if (rightBottomCornerY > leftBottomCornerY) {
                rightBottomCornerY += 1.0;
                rightTopCornerY += 1.0;
            } else {
                leftBottomCornerY += 1.0;
                leftTopCornerY += 1.0;
            }
        } else if (widthZ && heightX) {
            if (leftTopCornerX > leftBottomCornerX) {
                leftTopCornerX += 1.0;
                rightTopCornerX += 1.0;
            } else {
                leftBottomCornerX += 1.0;
                rightBottomCornerX += 1.0;
            }

            if (rightBottomCornerZ > leftBottomCornerZ) {
                rightBottomCornerZ += 1.0;
                rightTopCornerZ += 1.0;
            } else {
                leftBottomCornerZ += 1.0;
                leftTopCornerZ += 1.0;
            }
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

        boolean isFrontFacing = dot > 0;

        if (dot < 0) {
            normalX = -normalX;
            normalY = -normalY;
            normalZ = -normalZ;
        }

        if (isFrontFacing) {
            leftBottomCornerX  += normalX;
            leftBottomCornerY += normalY;
            leftBottomCornerZ += normalZ;

            leftTopCornerX += normalX;
            leftTopCornerY += normalY;
            leftTopCornerZ += normalZ;

            rightBottomCornerX += normalX;
            rightBottomCornerY += normalY;
            rightBottomCornerZ += normalZ;

            rightTopCornerX += normalX;
            rightTopCornerY += normalY;
            rightTopCornerZ += normalZ;
        }

        double againstZFighting = 0.05;

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
            vertexConsumer.addVertex(pose, (float)leftBottomCornerX,   (float)leftBottomCornerY,  (float)leftBottomCornerZ).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal((float)normalX, (float)normalY, (float)normalZ);
            vertexConsumer.addVertex(pose, (float)leftTopCornerX,         (float)leftTopCornerY,     (float)leftTopCornerZ).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal((float)normalX, (float)normalY, (float)normalZ);
            vertexConsumer.addVertex(pose, (float)rightTopCornerX,       (float)rightTopCornerY,    (float)rightTopCornerZ).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal((float)normalX, (float)normalY, (float)normalZ);
            vertexConsumer.addVertex(pose, (float)rightBottomCornerX, (float)rightBottomCornerY, (float)rightBottomCornerZ).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal((float)normalX, (float)normalY, (float)normalZ);
        } else {
            vertexConsumer.addVertex(pose, (float)leftBottomCornerX,   (float)leftBottomCornerY,  (float)leftBottomCornerZ).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal((float)normalX, (float)normalY, (float)normalZ);
            vertexConsumer.addVertex(pose, (float)leftTopCornerX,         (float)leftTopCornerY,     (float)leftTopCornerZ).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal((float)normalX, (float)normalY, (float)normalZ);
            vertexConsumer.addVertex(pose, (float)rightTopCornerX,       (float)rightTopCornerY,    (float)rightTopCornerZ).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal((float)normalX, (float)normalY, (float)normalZ);
            vertexConsumer.addVertex(pose, (float)rightBottomCornerX, (float)rightBottomCornerY, (float)rightBottomCornerZ).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal((float)normalX, (float)normalY, (float)normalZ);
        }
    }
}