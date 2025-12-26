package me.kall.narutoloading.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.core.detection.inworld.ScreenChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;

public class NarutoInWorldRenderer extends NarutoRenderer {
    public static final NarutoInWorldRenderer INSTANCE = new NarutoInWorldRenderer();

    public void onRenderLevel(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        ResourceLocation dimension = level.dimension().location();
        ObjectSet<ScreenChecker.Screen> screens =  ScreenChecker.SCREENS.get(dimension);
        if (screens == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        for (ScreenChecker.Screen screen : screens) {
            renderImage(poseStack, bufferSource, this.nextFrame(), screen);
        }
    }

    private static void renderImage(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, ResourceLocation textureLocation, ScreenChecker.@NotNull Screen screen) {
        RenderType renderType = RenderType.entityTranslucentCull(textureLocation);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        Vec3 leftBottom = new Vec3(
                screen.leftBottomCorner().getX(),
                screen.leftBottomCorner().getY(),
                screen.leftBottomCorner().getZ()
        );
        Vec3 leftTop = new Vec3(
                screen.leftTopCorner().getX(),
                screen.leftTopCorner().getY(),
                screen.leftTopCorner().getZ()
        );
        Vec3 rightBottom = new Vec3(
                screen.rightBottomCorner().getX(),
                screen.rightBottomCorner().getY(),
                screen.rightBottomCorner().getZ()
        );
        Vec3 rightTop = new Vec3(
                screen.rightTopCorner().getX(),
                screen.rightTopCorner().getY(),
                screen.rightTopCorner().getZ()
        );

        Vec3 normal = leftTop.subtract(leftBottom).cross(rightBottom.subtract(leftBottom)).normalize();

        poseStack.pushPose();

        vertexConsumer
                .vertex(poseStack.last().pose(), (float)leftBottom.x, (float)leftBottom.y, (float)leftBottom.z)
                .color(255, 255, 255, 255)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(poseStack.last().normal(), (float)normal.x, (float)normal.y, (float)normal.z)
                .endVertex();
        vertexConsumer
                .vertex(poseStack.last().pose(), (float)leftTop.x, (float)leftTop.y, (float)leftTop.z)
                .color(255, 255, 255, 255)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(poseStack.last().normal(), (float)normal.x, (float)normal.y, (float)normal.z)
                .endVertex();
        vertexConsumer
                .vertex(poseStack.last().pose(), (float)rightTop.x, (float)rightTop.y, (float)rightTop.z)
                .color(255, 255, 255, 255)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(poseStack.last().normal(), (float)normal.x, (float)normal.y, (float)normal.z)
                .endVertex();
        vertexConsumer
                .vertex(poseStack.last().pose(), (float)rightBottom.x, (float)rightBottom.y, (float)rightBottom.z)
                .color(255, 255, 255, 255)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(poseStack.last().normal(), (float)normal.x, (float)normal.y, (float)normal.z)
                .endVertex();

        poseStack.popPose();
    }
}
