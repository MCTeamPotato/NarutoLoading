package me.kall.narutoloading.core;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.detection.KeyChecker;
import me.kall.narutoloading.core.detection.WindowSizeChecker;
import me.kall.narutoloading.data.VideoArgs;
import me.kall.narutoloading.core.execution.NarutoAudioExecutor;
import me.kall.narutoloading.core.execution.NarutoVideoExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NarutoRenderer {
    public static final NarutoRenderer INSTANCE = new NarutoRenderer();

    public @Nullable DynamicTexture dynamicTexture;
    public @Nullable ResourceLocation textureLocation;

    public final LifetimeController lifetime = new LifetimeController();

    private static @NotNull NativeImage buildImage(byte @NotNull [] buffer) {
        NativeImage image = new NativeImage(VideoArgs.width(), VideoArgs.height(), false);
        for (int i = 0; i < buffer.length; i += 3) {
            int b = buffer[i] & 0xFF;
            int g = buffer[i + 1] & 0xFF;
            int r = buffer[i + 2] & 0xFF;
            int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
            image.setPixelRGBA(i / 3 % VideoArgs.width(), i / 3 / VideoArgs.width(), argb);
        }
        return image;
    }

    public void setup() {
        if (this.dynamicTexture != null) return;
        this.dynamicTexture = new DynamicTexture(VideoArgs.width(), VideoArgs.height(), false);
        if (this.textureLocation == null) {
            this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
            NarutoLoading.LOGGER.info("NarutoRenderer texture location initialized: {}", this.textureLocation.toString());
        }
        NarutoAudioExecutor.INSTANCE.setup();
        NarutoVideoExecutor.INSTANCE.setup();
        this.lifetime.start();
    }

    private ResourceLocation nextFrame() {
        if (this.dynamicTexture == null) this.setup();
        if (this.lifetime.shouldUpdateFrame(VideoArgs.fps())) {
            byte[] frame = NarutoVideoExecutor.INSTANCE.fetchImage(this.lifetime.elapsedSeconds());
            if (frame != null) {
                NativeImage image = buildImage(frame);
                this.dynamicTexture.setPixels(image);
                this.dynamicTexture.upload();
                image.close();
            }
        }

        return this.textureLocation;
    }

    public boolean isRunning() {
        return this.lifetime.isRunning();
    }

    public void renderFrame(GuiGraphics graphics) {
        if (this.isEnabled()) {
            ResourceLocation texture = this.nextFrame();

            int w = graphics.guiWidth();
            int h = graphics.guiHeight();

            this.lifetime.tick();

            graphics.blit(texture, 0, 0, 0, 0, w, h, w, h);

            KeyChecker.reload(this);
            WindowSizeChecker.resize(this);
            this.lifetime.syncSoundEngine();
            this.lifetime.lagSpikeRestart();
            this.lifetime.endRestart();
        }
    }

    private boolean isEnabled() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GenericDirtMessageScreen) return true;
        if (VideoArgs.width() == 0 || VideoArgs.height() == 0) return false;
        if (minecraft.level != null || !minecraft.isRunning()) {
            this.shutdown();
            return false;
        }
        return true;
    }

    public void shutdown() {
        NarutoAudioExecutor.INSTANCE.shutdown();
        NarutoVideoExecutor.INSTANCE.shutdown();

        if (this.dynamicTexture != null) {
            this.dynamicTexture.close();
            this.dynamicTexture = null;
        }

        if (this.textureLocation != null) {
            Minecraft.getInstance().getTextureManager().release(this.textureLocation);
            this.textureLocation = null;
        }

        this.lifetime.stop();
    }
}
