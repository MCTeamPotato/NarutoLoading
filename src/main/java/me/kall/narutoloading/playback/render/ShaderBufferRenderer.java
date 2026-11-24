package me.kall.narutoloading.playback.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.client.context.Graphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class ShaderBufferRenderer implements FrameRenderer<ByteBuffer> {
    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "yuv");

    private final Loading loading = new Loading();

    private @Nullable YuvTextures planes;
    private @Nullable ShaderInstance shader;

    @Override
    public void setup(int width, int height, double seekTo, boolean ready) {
        if (this.shader == null) this.shader = loadShader();

        if (this.planes != null && this.planes.width() == width && this.planes.height() == height) return;

        if (this.planes != null) this.planes.close();
        this.planes = new YuvTextures(width, height);

        if (!ready) this.planes.upload(this.loading.get(width, height));
    }

    @Override
    public void update(int width, int height, @Nullable ByteBuffer frame) {
        if (frame == null || this.planes == null) return;
        this.planes.upload(frame);
    }

    @Override
    public void render() {
        YuvTextures planes = this.planes;
        ShaderInstance shader = this.shader;
        GuiGraphics graphics = Graphics.get();

        if (planes == null || shader == null || graphics == null || planes.isEmpty()) return;

        RenderSystem.assertOnRenderThread();

        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, planes.texture(0));
        RenderSystem.setShaderTexture(1, planes.texture(1));
        RenderSystem.setShaderTexture(2, planes.texture(2));

        float width = graphics.guiWidth();
        float height = graphics.guiHeight();
        Matrix4f pose = graphics.pose().last().pose();

        try (ByteBufferBuilder byteBuffer = new ByteBufferBuilder(256)) {
            BufferBuilder builder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.addVertex(pose, 0F, height, 0F).setUv(0F, 1F);
            builder.addVertex(pose, width, height, 0F).setUv(1F, 1F);
            builder.addVertex(pose, width, 0F, 0F).setUv(1F, 0F);
            builder.addVertex(pose, 0F, 0F, 0F).setUv(0F, 0F);

            try (MeshData mesh = builder.build()) {
                CustomShaderDraw.draw(shader, mesh, false);
            }
        }
    }

    @Override
    public void shutdown() {
        if (this.planes != null) {
            this.planes.close();
            this.planes = null;
        }

        if (this.shader != null) {
            this.shader.close();
            this.shader = null;
        }
    }

    private static @Nullable ShaderInstance loadShader() {
        try {
            return new ShaderInstance(Minecraft.getInstance().getResourceManager(), SHADER, DefaultVertexFormat.POSITION_TEX);
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Could not load the yuv shader: " + exception.getMessage());
            return null;
        }
    }
}
