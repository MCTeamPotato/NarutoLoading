package me.kall.narutoloading.wall.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.playback.render.ImageFrameRenderer;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.client.cull.SurfaceTracker;
import me.kall.narutoloading.wall.client.light.WallLights;
import me.kall.narutoloading.wall.geometry.WallGeometry;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class WallImageRenderer extends ImageFrameRenderer {
    private final Wall wall;

    public WallImageRenderer(@NotNull Wall wall) {
        this.wall = wall;
    }

    @Override
    @Contract(" -> new")
    protected @NotNull ResourceLocation setLocation() {
        return ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "wall_" + this.wall.id);
    }

    @Override
    public void update(int width, int height, @Nullable NativeImage frame) {
        super.update(width, height, frame);
        if (frame != null) WallLights.sample(this.wall, width, height, null, frame);
    }

    @Override
    public void render() {
        PoseStack poseStack = WallRenderContext.poseStack();
        Camera camera = WallRenderContext.camera();
        MultiBufferSource.BufferSource bufferSource = WallRenderContext.bufferSource();
        if (poseStack == null || camera == null || bufferSource == null || this.textureLocation == null || this.dynamicTexture == null) return;

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        RenderType renderType = RenderType.entityTranslucent(this.textureLocation);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        WallGeometry.Coords coords = WallGeometry.computeCoords(this.wall, camera);
        SurfaceTracker.track(this.wall.dimension, coords);

        vertex(consumer, pose, normal, coords.bottomFromX(), coords.bottomFromY(), coords.bottomFromZ(), coords.u0(), coords.v0(), coords.normalX(), coords.normalY(), coords.normalZ());
        vertex(consumer, pose, normal, coords.bottomToX(), coords.bottomToY(), coords.bottomToZ(), coords.u1(), coords.v1(), coords.normalX(), coords.normalY(), coords.normalZ());
        vertex(consumer, pose, normal, coords.topToX(), coords.topToY(), coords.topToZ(), coords.u2(), coords.v2(), coords.normalX(), coords.normalY(), coords.normalZ());
        vertex(consumer, pose, normal, coords.topFromX(), coords.topFromY(), coords.topFromZ(), coords.u3(), coords.v3(), coords.normalX(), coords.normalY(), coords.normalZ());

        bufferSource.endBatch(renderType);
    }

    private static void vertex(@NotNull VertexConsumer consumer, Matrix4f pose, Matrix3f normal, double x, double y, double z, float u, float v, double normalX, double normalY, double normalZ) {
        consumer.vertex(pose, (float) x, (float) y, (float) z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, (float) normalX, (float) normalY, (float) normalZ)
                .endVertex();
    }
}
