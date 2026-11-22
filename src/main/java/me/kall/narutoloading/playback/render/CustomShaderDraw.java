package me.kall.narutoloading.playback.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11C;

/**
 * Draws a buffer with a custom {@link ShaderInstance}.
 * <p>
 * The vanilla {@code BufferUploader.drawWithShader} path is not usable while an Iris-based shader
 * pack is active: Iris injects at the end of {@code ShaderInstance.apply} and disables the color
 * and depth write masks for every shader it does not own (its own {@code ExtendedShader} and
 * {@code FallbackShader} are exempt). A custom shader such as the YUV one is therefore applied but
 * its draw writes no pixels, which looks like a completely black video.
 * <p>
 * This helper mirrors the vanilla uniform setup, applies the shader, then restores the write masks
 * with raw GL calls. Raw calls are required because Iris defers {@code GlStateManager._colorMask} and
 * {@code _depthMask} while its lock is active. The masks are restored again by
 * {@code ShaderInstance.clear()}, so the normal GUI/world state is not leaked.
 */
public final class CustomShaderDraw {
    private CustomShaderDraw() {}

    /**
     * Draws one prepared buffer with the given shader.
     *
     * @param shader     the custom shader
     * @param buffer     the rendered buffer
     * @param depthWrite whether the draw should write depth (world geometry) or not (GUI)
     */
    public static void draw(ShaderInstance shader, BufferBuilder.RenderedBuffer buffer, boolean depthWrite) {
        for (int i = 0; i < 12; i++) shader.setSampler("Sampler" + i, RenderSystem.getShaderTexture(i));

        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());

        RenderSystem.setupShaderLights(shader);
        shader.apply();

        GL11C.glColorMask(true, true, true, true);
        GL11C.glDepthMask(depthWrite);

        BufferUploader.draw(buffer);
        shader.clear();
    }
}
