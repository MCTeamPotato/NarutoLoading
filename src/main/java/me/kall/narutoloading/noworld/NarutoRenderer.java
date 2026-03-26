package me.kall.narutoloading.noworld;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoTV;
import me.kall.narutoloading.core.executor.RestartExecutor;
import me.kall.narutoloading.core.executor.audio.NarutoAudioExecutor;
import me.kall.narutoloading.core.executor.video.NarutoVideoExecutor;
import me.kall.narutoloading.data.NarutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class NarutoRenderer extends NarutoTV<NativeImage, DynamicTexture, ResourceLocation> {
    public static final NarutoRenderer INSTANCE = new NarutoRenderer();

    @Override
    public void createVideo() {
        Runnable onLagSpike = () -> {
            if (this.lifetime != null) this.lifetime.lagSpikeDetected = true;
        };

        this.videoExecutor = new NarutoVideoExecutor(() -> onLagSpike, this.absoluteVideoPath(), () -> NarutoConfig.WIDTH, () -> NarutoConfig.HEIGHT, () -> this.fps);
    }

    @Override
    public void createAudio() {
        this.audioExecutor = new NarutoAudioExecutor(() -> () -> RestartExecutor.schedule(this::cleanup, this::init), this.absoluteVideoPath(), this.absoluteAudioPath());
    }

    @Override
    public boolean isRunnable() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen instanceof WinScreen || screen instanceof GenericDirtMessageScreen) return true;
        if (minecraft.getOverlay() instanceof LoadingOverlay) return true;
        if (minecraft.isPaused()) return false;

        if (minecraft.level != null) {
            this.cleanup();
            return false;
        }

        return minecraft.isRunning();
    }

    @Override
    public void createTexture() {
        if (this.texture == null) this.texture = new DynamicTexture(NarutoConfig.WIDTH, NarutoConfig.HEIGHT, false);
        if (this.textureLocation == null) this.textureLocation = this.textureManager().register("naruto_video_dynamic", this.texture);
    }

    @Override
    public void consumeFrame(NativeImage nativeImage, @NotNull DynamicTexture dynamicTexture) {
        dynamicTexture.setPixels(nativeImage);
        dynamicTexture.upload();
        nativeImage.close();
    }

    @Override
    public void renderFrame(ResourceLocation textureLocation) {
        GuiGraphics graphics = NarutoLoading.GUI_GRAPHICS.get();
        if (graphics == null) return;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        assert this.textureLocation != null;
        graphics.blit(this.textureLocation, 0, 0, 0, 0, width, height, width, height);
    }

    @Override
    public void cleanupTexture() {
        if (this.texture != null) {
            this.texture.close();
            this.texture = null;
        }

        if (this.textureLocation != null) {
            this.textureManager().release(this.textureLocation);
            this.textureLocation = null;
        }
    }

    private @NotNull TextureManager textureManager() {
        return Minecraft.getInstance().getTextureManager();
    }
}
