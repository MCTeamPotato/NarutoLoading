package me.kall.narutoloading.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.core.detection.inworld.ScreenChecker;
import me.kall.narutoloading.data.VideoArgs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class NarutoInWorldRenderer extends NarutoRenderer {
    public static final NarutoInWorldRenderer INSTANCE = new NarutoInWorldRenderer();

    public final Object2ObjectMap<ResourceLocation, ObjectSet<ScreenChecker.Screen>> screens = new Object2ObjectOpenHashMap<>();

    public void onRenderTick(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START && this.isRunning()) {
            this.renderFrame(null);
            if (Minecraft.getInstance().level == null) this.shutdown();
        }
    }

    public void onRenderLevel(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!this.isRunning()) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        ResourceLocation dimension = level.dimension().location();
        ObjectSet<ScreenChecker.Screen> screens = this.screens.get(dimension);
        if (screens == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Vec3 cameraPos = event.getCamera().getPosition();

        for (ScreenChecker.Screen screen : screens) {
            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, - cameraPos.y, - cameraPos.z);
            this.renderImage(poseStack, bufferSource, this.nextFrame(), screen);
            poseStack.popPose();
        }
    }

    public void renderImage(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, ResourceLocation textureLocation, ScreenChecker.@NotNull Screen screen) {
        RenderType renderType = RenderType.entityTranslucentCull(textureLocation);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        BlockPos leftBottomCorner = screen.leftBottomCorner();
        BlockPos leftTopCorner = screen.leftTopCorner();
        BlockPos rightBottomCorner = screen.rightBottomCorner();
        BlockPos rightTopCorner = screen.rightTopCorner();

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
    public void renderFrame(@Nullable GuiGraphics graphics) {
        if (this.isEnabled()) {
            this.lifetime.tick();
            this.keyChecker.reload();
            this.lifetime.lagSpikeRestart();
            this.lifetime.endRestart();
        }
    }

    @Override
    public boolean isEnabled() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GenericDirtMessageScreen) return false;
        if (VideoArgs.width() == 0 || VideoArgs.height() == 0) return false;
        return minecraft.level != null && minecraft.isRunning();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.screens.clear();
    }
}
