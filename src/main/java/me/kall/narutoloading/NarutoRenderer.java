package me.kall.narutoloading;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class NarutoRenderer {
    private @Nullable DynamicTexture dynamicTexture;
    private @Nullable ResourceLocation textureLocation;
    private long last = 0;

    private long start = -1L;
    private long elapsed = 0L;

    private void setup() {
        if (this.dynamicTexture != null) return;
        this.dynamicTexture = new DynamicTexture(854, 480, false);
        if (this.textureLocation == null) this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", dynamicTexture);
        NarutoLoading.VIDEO.setup();
    }

    private @Nullable ResourceLocation nextFrame() {
        if (this.dynamicTexture == null) setup();
        long now = System.currentTimeMillis();
        if (now - this.last >= 1000 / NarutoLoading.FPS) {
            this.last = now;
            NativeImage frame = NarutoLoading.VIDEO.fetchImage(this.elapsed / 1000L);
            if (frame != null) {
                this.dynamicTexture.setPixels(frame);
                this.dynamicTexture.upload();
                frame.close();
            }
        }

        return this.textureLocation;
    }

    public void renderFrame(GuiGraphics graphics) {
        if (this.canRender()){
            this.checkReload();
            ResourceLocation texture = this.nextFrame();
            if (texture != null) {
                int w = graphics.guiWidth();
                int h = graphics.guiHeight();

                if (this.start == -1L) this.start = System.currentTimeMillis();
                this.elapsed = System.currentTimeMillis() - this.start;

                graphics.blit(texture, 0, 0, 0, 0, w, h, w, h);
            }
        }
    }

    private boolean canRender() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null || !minecraft.isRunning()) {
            this.shutdown();
            return false;
        }
         return true;
    }

    private void checkReload() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean isKeyReload = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F12) == GLFW.GLFW_PRESS;
        boolean isEndReload = NarutoLoading.VIDEO.frameQueue != null && NarutoLoading.VIDEO.frameQueue.isEmpty();

        if (isKeyReload || isEndReload) {
            this.shutdown();
            this.setup();
        }
    }

    private void shutdown() {
        NarutoLoading.VIDEO.shutdown();
        if (this.dynamicTexture != null) {
            this.dynamicTexture.close();
            this.dynamicTexture = null;
        }
        this.textureLocation = null;
        this.last = 0;

        this.start = -1L;
        this.elapsed = 0L;
    }
}
