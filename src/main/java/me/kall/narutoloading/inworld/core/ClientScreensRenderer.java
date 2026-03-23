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

            renderScreen(poseStack, bufferSource, renderer, camera);

            poseStack.popPose();
        }
    }

    private static void renderScreen(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, @NotNull NarutoInWorldRenderer renderer, Vec3 camera) {
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
            leftBottomCornerX += normalX;
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

        boolean isHorizontal = Math.abs(normalY) > 0.99;
        int rot = 0;

        if (isHorizontal) {
            double side1 = Math.sqrt(leftCornerDistX * leftCornerDistX + leftCornerDistY * leftCornerDistY + leftCornerDistZ * leftCornerDistZ);
            double side2 = Math.sqrt(rightCornerDistX * rightCornerDistX + rightCornerDistY * rightCornerDistY + rightCornerDistZ * rightCornerDistZ);
            boolean isSquare = Math.abs(side1 - side2) < 0.1;

            boolean videoPortrait = inWorldScreen.videoWidth() < inWorldScreen.videoHeight();

            double midBottomX = (leftBottomCornerX + rightBottomCornerX) / 2.0;
            double midBottomZ = (leftBottomCornerZ + rightBottomCornerZ) / 2.0;
            double midLeftX = (leftBottomCornerX + leftTopCornerX) / 2.0;
            double midLeftZ = (leftBottomCornerZ + leftTopCornerZ) / 2.0;
            double midTopX = (leftTopCornerX + rightTopCornerX) / 2.0;
            double midTopZ = (leftTopCornerZ + rightTopCornerZ) / 2.0;
            double midRightX = (rightBottomCornerX + rightTopCornerX) / 2.0;
            double midRightZ = (rightBottomCornerZ + rightTopCornerZ) / 2.0;

            double dirX = toCameraX;
            double dirZ = toCameraZ;
            double dirLen = Math.sqrt(dirX * dirX + dirZ * dirZ);
            if (dirLen > 0.001) {
                dirX /= dirLen;
                dirZ /= dirLen;
            } else {
                dirX = 0; dirZ = 0;
            }

            double[] dotToSide = new double[4];

            double vx = midBottomX - centerX;
            double vz = midBottomZ - centerZ;
            double vlen = Math.sqrt(vx * vx + vz * vz);
            if (vlen > 0.001) {
                vx /= vlen; vz /= vlen;
                dotToSide[0] = dirX * vx + dirZ * vz;
            } else dotToSide[0] = -1;

            vx = midLeftX - centerX;
            vz = midLeftZ - centerZ;
            vlen = Math.sqrt(vx * vx + vz * vz);
            if (vlen > 0.001) {
                vx /= vlen; vz /= vlen;
                dotToSide[1] = dirX * vx + dirZ * vz;
            } else dotToSide[1] = -1;

            vx = midTopX - centerX;
            vz = midTopZ - centerZ;
            vlen = Math.sqrt(vx * vx + vz * vz);
            if (vlen > 0.001) {
                vx /= vlen; vz /= vlen;
                dotToSide[2] = dirX * vx + dirZ * vz;
            } else dotToSide[2] = -1;

            vx = midRightX - centerX;
            vz = midRightZ - centerZ;
            vlen = Math.sqrt(vx * vx + vz * vz);
            if (vlen > 0.001) {
                vx /= vlen; vz /= vlen;
                dotToSide[3] = dirX * vx + dirZ * vz;
            } else dotToSide[3] = -1;

            int[] allowed;
            if (isSquare) {
                allowed = new int[]{0, 1, 2, 3};
            } else {
                allowed = ((side1 < side2) == videoPortrait) ? new int[]{1, 3} : new int[]{0, 2};
            }

            int bestEdge = allowed[0];
            double maxD = dotToSide[allowed[0]];
            for (int e : allowed) {
                if (dotToSide[e] > maxD) {
                    maxD = dotToSide[e];
                    bestEdge = e;
                }
            }
            rot = (4 - bestEdge) % 4;
        }

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMat = poseStack.last().normal();

        double[][] uv = new double[4][2];
        if (isFrontFacing) {
            uv[0][0] = 1; uv[0][1] = 1;
            uv[1][0] = 1; uv[1][1] = 0;
            uv[2][0] = 0; uv[2][1] = 0;
            uv[3][0] = 0;
        } else {
            uv[0][0] = 0; uv[0][1] = 1;
            uv[1][0] = 0; uv[1][1] = 0;
            uv[2][0] = 1; uv[2][1] = 0;
            uv[3][0] = 1;
        }
        uv[3][1] = 1;

        if (isHorizontal) {
            for (int r = 0; r < rot; r++) {
                for (int i = 0; i < 4; i++) {
                    double oldU = uv[i][0];
                    double oldV = uv[i][1];
                    uv[i][0] = oldV;
                    uv[i][1] = 1.0 - oldU;
                }
            }
        }

        if (isFrontFacing) {
            vertexConsumer.vertex(pose, (float) leftBottomCornerX, (float) leftBottomCornerY, (float) leftBottomCornerZ).color(255, 255, 255, 255).uv((float) uv[0][0], (float) uv[0][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) leftTopCornerX, (float) leftTopCornerY, (float) leftTopCornerZ).color(255, 255, 255, 255).uv((float) uv[1][0], (float) uv[1][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) rightTopCornerX, (float) rightTopCornerY, (float) rightTopCornerZ).color(255, 255, 255, 255).uv((float) uv[2][0], (float) uv[2][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) rightBottomCornerX, (float) rightBottomCornerY, (float) rightBottomCornerZ).color(255, 255, 255, 255).uv((float) uv[3][0], (float) uv[3][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
        } else {
            vertexConsumer.vertex(pose, (float) leftBottomCornerX, (float) leftBottomCornerY, (float) leftBottomCornerZ).color(255, 255, 255, 255).uv((float) uv[0][0], (float) uv[0][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) leftTopCornerX, (float) leftTopCornerY, (float) leftTopCornerZ).color(255, 255, 255, 255).uv((float) uv[1][0], (float) uv[1][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) rightTopCornerX, (float) rightTopCornerY, (float) rightTopCornerZ).color(255, 255, 255, 255).uv((float) uv[2][0], (float) uv[2][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) rightBottomCornerX, (float) rightBottomCornerY, (float) rightBottomCornerZ).color(255, 255, 255, 255).uv((float) uv[3][0], (float) uv[3][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
        }
    }

}