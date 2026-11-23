package me.kall.narutoloading.wall.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.playback.render.CustomShaderDraw;
import me.kall.narutoloading.playback.render.FrameRenderer;
import me.kall.narutoloading.playback.render.Loading;
import me.kall.narutoloading.playback.render.YuvTextures;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.client.cull.SurfaceTracker;
import me.kall.narutoloading.wall.client.light.WallLights;
import me.kall.narutoloading.wall.geometry.WallGeometry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL32C.*;

/**
 * Wall renderer that keeps the decoded frame as three YUV planes and converts it to an RGBA
 * texture on the GPU before drawing that texture with a vanilla {@link RenderType}.
 * <p>
 * The conversion step is deliberate: a custom shader drawn directly during the world pass is not
 * part of Iris/EmberLens' g-buffer pipeline and disappears under a shader pack, even when its
 * depth/color lock is bypassed. Drawing the result through {@code entityTranslucent} lets the
 * shader pack composite the wall like any other translucent block.
 */
public final class WallBufferRenderer implements FrameRenderer<ByteBuffer> {
    private final Wall wall;
    private final Loading loading = new Loading();

    private @Nullable YuvTextures planes;
    private @Nullable DynamicTexture texture;
    private @Nullable ResourceLocation location;
    private int framebuffer;
    private int width;
    private int height;

    public WallBufferRenderer(@NotNull Wall wall) {
        this.wall = wall;
    }

    @Override
    public void setup(int width, int height, double seekTo, boolean ready) {
        if (this.planes != null && this.width == width && this.height == height) return;

        this.width = width;
        this.height = height;

        this.release();

        this.planes = new YuvTextures(width, height);
        this.texture = new DynamicTexture(width, height, false);
        this.location = ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "wall_buffer_" + this.wall.id);
        Minecraft.getInstance().getTextureManager().register(this.location, this.texture);

        this.framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, this.framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.texture.getId(), 0);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        if (status != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("[NarutoLoading] Wall framebuffer is incomplete: " + status);
            return;
        }

        if (!ready) {
            this.planes.upload(this.loading.get(width, height));
            this.convert();
        }
    }

    @Override
    public void update(int width, int height, @Nullable ByteBuffer frame) {
        if (frame == null || this.planes == null) return;

        this.planes.upload(frame);
        this.convert();
        WallLights.sample(this.wall, width, height, frame);
    }

    @Override
    public void render() {
        ResourceLocation location = this.location;
        PoseStack poseStack = WallRenderContext.poseStack();
        Camera camera = WallRenderContext.camera();
        MultiBufferSource.BufferSource bufferSource = WallRenderContext.bufferSource();

        if (location == null || poseStack == null || camera == null || bufferSource == null) return;

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        RenderType renderType = RenderType.entityTranslucent(location);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        WallGeometry.Coords coords = WallGeometry.computeCoords(this.wall, camera);
        SurfaceTracker.track(this.wall.dimension, coords);

        vertex(consumer, pose, normal, coords.bottomFromX(), coords.bottomFromY(), coords.bottomFromZ(), coords.u0(), coords.v0(), coords.normalX(), coords.normalY(), coords.normalZ());
        vertex(consumer, pose, normal, coords.bottomToX(), coords.bottomToY(), coords.bottomToZ(), coords.u1(), coords.v1(), coords.normalX(), coords.normalY(), coords.normalZ());
        vertex(consumer, pose, normal, coords.topToX(), coords.topToY(), coords.topToZ(), coords.u2(), coords.v2(), coords.normalX(), coords.normalY(), coords.normalZ());
        vertex(consumer, pose, normal, coords.topFromX(), coords.topFromY(), coords.topFromZ(), coords.u3(), coords.v3(), coords.normalX(), coords.normalY(), coords.normalZ());

        bufferSource.endBatch(renderType);
    }

    @Override
    public void shutdown() {
        this.release();
    }

    private void convert() {
        YuvTextures planes = this.planes;
        ShaderInstance shader = WallShaders.shader();

        if (planes == null || shader == null || this.framebuffer == 0 || this.texture == null) return;

        int previousFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);

        boolean depth = glIsEnabled(GL_DEPTH_TEST);
        boolean blend = glIsEnabled(GL_BLEND);
        boolean cull = glIsEnabled(GL_CULL_FACE);

        glBindFramebuffer(GL_FRAMEBUFFER, this.framebuffer);
        glViewport(0, 0, this.width, this.height);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);

        RenderSystem.setShaderTexture(0, planes.texture(0));
        RenderSystem.setShaderTexture(1, planes.texture(1));
        RenderSystem.setShaderTexture(2, planes.texture(2));

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0F, this.width, this.height, 0F, -1F, 1F), VertexSorting.ORTHOGRAPHIC_Z);

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        Matrix4f pose = new Matrix4f();

        BufferBuilder builder = new BufferBuilder(256);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        builder.vertex(pose, 0F, this.height, 0F).uv(0F, 0F).endVertex();
        builder.vertex(pose, this.width, this.height, 0F).uv(1F, 0F).endVertex();
        builder.vertex(pose, this.width, 0F, 0F).uv(1F, 1F).endVertex();
        builder.vertex(pose, 0F, 0F, 0F).uv(0F, 1F).endVertex();
        CustomShaderDraw.draw(shader, builder.end(), false);

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();

        glBindFramebuffer(GL_FRAMEBUFFER, previousFramebuffer);
        glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        if (depth) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
        if (blend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        if (cull) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
    }

    private void release() {
        if (this.location != null) {
            Minecraft.getInstance().getTextureManager().release(this.location);
            this.location = null;
        }

        if (this.texture != null) {
            this.texture.close();
            this.texture = null;
        }

        if (this.planes != null) {
            this.planes.close();
            this.planes = null;
        }

        if (this.framebuffer != 0) {
            glDeleteFramebuffers(this.framebuffer);
            this.framebuffer = 0;
        }
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
