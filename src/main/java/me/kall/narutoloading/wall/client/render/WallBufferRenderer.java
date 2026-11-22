package me.kall.narutoloading.wall.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import me.kall.narutoloading.playback.render.FrameRenderer;
import me.kall.narutoloading.playback.render.Loading;
import me.kall.narutoloading.playback.render.YuvTextures;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.client.cull.SurfaceTracker;
import me.kall.narutoloading.wall.client.light.WallLights;
import me.kall.narutoloading.wall.geometry.WallGeometry;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL32C.*;

public final class WallBufferRenderer implements FrameRenderer<ByteBuffer> {
    private final Wall wall;
    private final Loading loading = new Loading();

    private @Nullable YuvTextures planes;

    public WallBufferRenderer(@NotNull Wall wall) {
        this.wall = wall;
    }

    @Override
    public void setup(int width, int height, double seekTo, boolean ready) {
        if (this.planes != null) this.planes.close();
        this.planes = new YuvTextures(width, height);

        if (!ready) this.planes.upload(this.loading.get(width, height));
    }

    @Override
    public void update(int width, int height, @Nullable ByteBuffer frame) {
        if (frame == null || this.planes == null) return;

        this.planes.upload(frame);
        WallLights.sample(this.wall, width, height, frame, null);
    }

    @Override
    public void render() {
        YuvTextures planes = this.planes;
        ShaderInstance shader = WallShaders.shader();
        PoseStack poseStack = WallRenderContext.poseStack();
        Camera camera = WallRenderContext.camera();

        if (planes == null || shader == null || poseStack == null || camera == null || planes.isEmpty()) return;

        RenderSystem.assertOnRenderThread();

        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, planes.texture(0));
        RenderSystem.setShaderTexture(1, planes.texture(1));
        RenderSystem.setShaderTexture(2, planes.texture(2));

        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean depthWrite = glGetBoolean(GL_DEPTH_WRITEMASK);
        boolean cull = glIsEnabled(GL_CULL_FACE);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        WallGeometry.Coords coords = WallGeometry.computeCoords(this.wall, camera);
        SurfaceTracker.track(this.wall.dimension, coords);
        Matrix4f pose = poseStack.last().pose();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        vertex(builder, pose, coords.bottomFromX(), coords.bottomFromY(), coords.bottomFromZ(), coords.u0(), coords.v0());
        vertex(builder, pose, coords.bottomToX(), coords.bottomToY(), coords.bottomToZ(), coords.u1(), coords.v1());
        vertex(builder, pose, coords.topToX(), coords.topToY(), coords.topToZ(), coords.u2(), coords.v2());
        vertex(builder, pose, coords.topFromX(), coords.topFromY(), coords.topFromZ(), coords.u3(), coords.v3());
        BufferUploader.drawWithShader(builder.end());

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.depthMask(depthWrite);
        if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
    }

    @Override
    public void shutdown() {
        if (this.planes == null) return;

        this.planes.close();
        this.planes = null;
    }

    private static void vertex(@NotNull BufferBuilder builder, Matrix4f pose, double x, double y, double z, float u, float v) {
        builder.vertex(pose, (float) x, (float) y, (float) z).uv(u, v).endVertex();
    }
}
