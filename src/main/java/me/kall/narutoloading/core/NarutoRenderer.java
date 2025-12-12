package me.kall.narutoloading.core;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.NarutoLoadingClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class NarutoRenderer {
    private @Nullable DynamicTexture dynamicTexture;
    private @Nullable ResourceLocation textureLocation;

    private long last = 0L;
    private long start = -1L;
    private long elapsed = 0L;

    private long frameCount = 0L;

    private int lastWidth = -1;
    private int lastHeight = -1;

    private int reloadCooldown = 0;
    private int resizeCooldown = 0;

    private boolean isRunning = false;

    public volatile boolean syncSoundEngine = false;

    private void setup() {
        if (this.dynamicTexture != null) return;
        this.dynamicTexture = new DynamicTexture(NarutoLoadingClient.Constants.width(), NarutoLoadingClient.Constants.height(), false);
        if (this.textureLocation == null) this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
        NarutoLoadingClient.AUDIO.setup();
        NarutoLoadingClient.VIDEO.setup();
        this.isRunning = true;
    }

    private ResourceLocation nextFrame() {
        if (this.dynamicTexture == null) this.setup();
        long now = System.currentTimeMillis();
        if (now - this.last >= 1000L / NarutoLoadingClient.Constants.fps()) {
            this.last = now;
            NativeImage frame = NarutoLoadingClient.VIDEO.fetchImage((double) this.elapsed / 1000D);
            if (frame != null) {
                this.dynamicTexture.setPixels(frame);
                this.dynamicTexture.upload();
                frame.close();
            }
        }

        return this.textureLocation;
    }

    public @Nullable ResourceLocation texture() {
        return this.textureLocation;
    }

    public void renderFrame(GuiGraphics graphics) {
        if (this.canRender()) {
            ResourceLocation texture = this.nextFrame();

            int w = graphics.guiWidth();
            int h = graphics.guiHeight();

            if (this.start == -1L) this.start = System.currentTimeMillis();
            this.elapsed = System.currentTimeMillis() - this.start;
            this.frameCount++;

            graphics.blit(texture, 0, 0, 0, 0, w, h, w, h);

            this.checkSize();
            this.keyReload();
            this.syncSoundEngine();
        }
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    private void syncSoundEngine() {
        if (this.syncSoundEngine) {
            this.syncSoundEngine = false;
            NarutoLoadingClient.AUDIO.setup(String.valueOf((double) this.elapsed / 1000D));
        }
    }

    private void checkSize() {
        if (this.resizeCooldown != 0) {
            this.resizeCooldown--;
            return;
        }
        int width = NarutoLoadingClient.Constants.width();
        int height = NarutoLoadingClient.Constants.height();
        if (this.lastWidth == -1 && this.lastHeight == -1) {
            this.lastWidth = width;
            this.lastHeight = height;
            return;
        }
        if (this.lastWidth != width || this.lastHeight != height) {
            NarutoLoading.LOGGER.info("Window size changed from [{}, {}] to [{}, {}]", this.lastWidth, this.lastHeight, width, height);
            this.resizeCooldown = 200;
            this.lastWidth = width;
            this.lastHeight = height;
            this.resize();
        }
    }

    private void resize() {
        String currentSecond = String.valueOf((double) this.elapsed / 1000D);
        NarutoLoading.LOGGER.info("Resizing Naruto Loading video from {} seconds", currentSecond);

        NarutoLoadingClient.VIDEO.shutdown(this.frameCount);
        NarutoLoadingClient.VIDEO.setup(currentSecond);

        if (this.dynamicTexture != null) this.dynamicTexture.close();

        this.dynamicTexture = new DynamicTexture(NarutoLoadingClient.Constants.width(), NarutoLoadingClient.Constants.height(), false);
        this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
    }

    private boolean canRender() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GenericDirtMessageScreen) return true;
        if (NarutoLoadingClient.Constants.width() == 0 && NarutoLoadingClient.Constants.height() == 0) return false;
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
        int keyState = GLFW.glfwGetKey(window, NarutoLoadingClient.NarutoConfig.RELOAD);

        if (keyState == GLFW.GLFW_PRESS) {
            this.reloadCooldown = 200;
            this.shutdown();
            this.setup();
        }
    }

    private void shutdown() {
        NarutoLoadingClient.AUDIO.shutdown();
        NarutoLoadingClient.VIDEO.shutdown();

        if (this.dynamicTexture != null) {
            this.dynamicTexture.close();
            this.dynamicTexture = null;
        }

        this.textureLocation = null;
        this.last = 0;

        this.start = -1L;
        this.elapsed = 0L;

        this.frameCount = 0L;

        this.isRunning = false;
    }
}
