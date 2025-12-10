package me.kall.narutoloading;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
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

    private long frameCount = 0L;

    private int lastWidth = -1;
    private int lastHeight = -1;

    private int reloadCooldown = 0;
    private int resizeCooldown = 0;

    private void setup() {
        if (this.dynamicTexture != null) return;
        this.dynamicTexture = new DynamicTexture(NarutoLoading.width(), NarutoLoading.height(), false);
        if (this.textureLocation == null) this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
        NarutoLoading.AUDIO.setup();
        NarutoLoading.VIDEO.setup();
    }

    private @Nullable ResourceLocation nextFrame() {
        if (this.dynamicTexture == null) setup();
        long now = System.currentTimeMillis();
        if (now - this.last >= 1000 / NarutoLoading.fps()) {
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
            ResourceLocation texture = this.nextFrame();
            if (texture != null) {
                int w = graphics.guiWidth();
                int h = graphics.guiHeight();

                if (this.start == -1L) this.start = System.currentTimeMillis();
                this.elapsed = System.currentTimeMillis() - this.start;
                this.frameCount++;

                graphics.blit(texture, 0, 0, 0, 0, w, h, w, h);

                this.checkSize();
                this.keyReload();
            }
        }
    }

    private void checkSize() {
        if (this.resizeCooldown != 0) {
            this.resizeCooldown--;
            return;
        }
        int width = NarutoLoading.width();
        int height = NarutoLoading.height();
        if (this.lastWidth == -1 && this.lastHeight == -1) {
            this.lastWidth = width;
            this.lastHeight = height;
            return;
        }
        if (this.lastWidth != width || this.lastHeight != height) {
            NarutoLoading.LOGGER.info("Window size changed from [{}, {}] to [{}, {}]", lastWidth, lastHeight, width, height);
            this.resizeCooldown = 200;
            this.lastWidth = width;
            this.lastHeight = height;
            this.resize();
        }
    }

    private void resize() {
        String sec = String.valueOf(Math.toIntExact(this.elapsed / 1000L));
        NarutoLoading.LOGGER.info("Resizing Naruto Loading video from {} seconds", sec);
        NarutoLoading.VIDEO.shutdown(this.frameCount);
        NarutoLoading.VIDEO.setup(sec);
        if (this.dynamicTexture != null) this.dynamicTexture.close();
        this.dynamicTexture = new DynamicTexture(NarutoLoading.width(), NarutoLoading.height(), false);
        this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
    }

    private boolean canRender() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GenericDirtMessageScreen) return true;
        if (minecraft.level != null || !minecraft.isRunning()) {
            this.shutdown();
            return false;
        }
         return true;
    }

    private void keyReload() {
        if (this.reloadCooldown != 0) {
            this.reloadCooldown--;
            return;
        }
        long window = Minecraft.getInstance().getWindow().getWindow();
        int keyStatus = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F12);

        if (keyStatus == GLFW.GLFW_PRESS) {
            this.reloadCooldown = 200;
            this.shutdown();
            this.setup();
        }
    }

    private void shutdown() {
        NarutoLoading.AUDIO.shutdown();
        NarutoLoading.VIDEO.shutdown();
        if (this.dynamicTexture != null) {
            this.dynamicTexture.close();
            this.dynamicTexture = null;
        }
        this.textureLocation = null;
        this.last = 0;

        this.start = -1L;
        this.elapsed = 0L;

        this.frameCount = 0L;
    }
}
