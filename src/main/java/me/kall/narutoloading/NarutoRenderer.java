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
        this.dynamicTexture = new DynamicTexture(NarutoLoading.width(), NarutoLoading.height(), false);
        if (this.textureLocation == null) this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", dynamicTexture);
        NarutoLoading.VIDEO.setup();
        NarutoLoading.AUDIO.setup();
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
            } else {
                this.shutdown();
                this.setup();
            }
        }

        return this.textureLocation;
    }

    public void renderFrame(GuiGraphics graphics) {
        if (this.canRender()){
            this.keyReload();
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

    private void keyReload() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        int keyStatus = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F12);

        if (keyStatus == GLFW.GLFW_PRESS) {
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
    }
}
