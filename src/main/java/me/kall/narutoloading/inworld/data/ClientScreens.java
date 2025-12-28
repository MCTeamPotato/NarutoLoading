package me.kall.narutoloading.inworld.data;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public class ClientScreens {
    public static final Object2ObjectMap<ResourceLocation, ObjectSet<ClientScreen>> CLIENT_SCREENS = new Object2ObjectOpenHashMap<>();

    @SubscribeEvent
    public static void renderTick(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (CLIENT_SCREENS.isEmpty()) return;
            for (ObjectSet<ClientScreen> clientScreens : CLIENT_SCREENS.values()) {
                for (ClientScreen clientScreen : clientScreens) {
                    clientScreen.renderer.renderFrame(null);
                }
            }
        }
    }

    @SubscribeEvent
    public static void renderLevel(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        ResourceLocation dimension = level.dimension().location();
        ObjectSet<ClientScreen> clientScreens = CLIENT_SCREENS.get(dimension);
        if (clientScreens == null || clientScreens.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();

        for (ClientScreen clientScreen : clientScreens) {
            poseStack.pushPose();
            poseStack.translate(-camera.x, - camera.y, - camera.z);

            ClientScreens.renderScreen(poseStack, bufferSource, clientScreen.renderer.nextFrame(), clientScreen.screen);

            poseStack.popPose();
        }
    }

    private static void renderScreen(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, ResourceLocation textureLocation, @NotNull InWorldScreen inWorldScreen) {
        RenderType renderType = RenderType.entityTranslucentCull(textureLocation);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

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

        vertexConsumer.vertex(pose, (float)leftBottomCornerX,   (float)leftBottomCornerY,  (float)leftBottomCornerZ).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
        vertexConsumer.vertex(pose, (float)leftTopCornerX,         (float)leftTopCornerY,     (float)leftTopCornerZ).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
        vertexConsumer.vertex(pose, (float)rightTopCornerX,       (float)rightTopCornerY,    (float)rightTopCornerZ).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
        vertexConsumer.vertex(pose, (float)rightBottomCornerX, (float)rightBottomCornerY, (float)rightBottomCornerZ).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, (float)normalX, (float)normalY, (float)normalZ).endVertex();
    }

    public record ClientScreen(InWorldScreen screen, NarutoInWorldRenderer renderer) {}
}
