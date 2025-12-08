package me.kall.narutoloading;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class NarutoRenderer {
    private static DynamicTexture dynamicTexture;
    private static ResourceLocation textureLocation;
    private static long lastFrame = 0;

    public static void setup() {
        if (dynamicTexture != null) return;
        dynamicTexture = new DynamicTexture(854, 480, false);
        if (textureLocation == null) textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", dynamicTexture);
        NarutoFrameExecutor.setup();
        System.out.println("NarutoRenderer sets up successfully.");
    }

    public static ResourceLocation nextFrame() {
        if (dynamicTexture == null) setup();
        long now = System.currentTimeMillis();
        if (now - lastFrame >= 1000 / NarutoLoading.FPS) {
            lastFrame = now;
            NativeImage frame = NarutoFrameExecutor.frameQueue.poll();
            if (frame != null) {
                dynamicTexture.setPixels(frame);
                dynamicTexture.upload();
                frame.close();
            } else {
                shutdown();
                setup();
            }
        }

        return textureLocation;
    }

    public static void renderFrame(GuiGraphics graphics) {
        if (Minecraft.getInstance().level != null) {
            shutdown();
            return;
        }
        if (!Minecraft.getInstance().isRunning()) {
            shutdown();
            return;
        }
        if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_F12) == GLFW.GLFW_PRESS) {
            shutdown();
            setup();
        }
        ResourceLocation texture = nextFrame();
        if (texture != null) {
            int w = graphics.guiWidth();
            int h = graphics.guiHeight();
            graphics.blit(texture, 0, 0, 0, 0, w, h, w, h);
        }
    }

    public static void shutdown() {
        NarutoFrameExecutor.shutdown();
        if (dynamicTexture != null) {
            dynamicTexture.close();
            dynamicTexture = null;
        }
        textureLocation = null;
        lastFrame = 0;
        System.out.println("NarutoRenderer shuts down successfully.");
    }
}
