package me.kall.narutoloading.noworld;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.LifetimeController;
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
            LifetimeController lifetime = this.lifetime.get();
            if (lifetime != null) lifetime.lagSpikeDetected.set(true);
        };
        this.videoExecutor.set(new NarutoVideoExecutor(() -> onLagSpike, this.absoluteVideoPath(), () -> NarutoConfig.WIDTH, () -> NarutoConfig.HEIGHT, this::getFps));
    }

    @Override
    public void createAudio() {
        this.audioExecutor.set(new NarutoAudioExecutor(() -> () -> RestartExecutor.schedule(this::restart), this.absoluteVideoPath(), this.absoluteAudioPath()));
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
        if (this.texture.get() == null) this.texture.set(new DynamicTexture(NarutoConfig.WIDTH, NarutoConfig.HEIGHT, false));
        if (this.textureLocation.get() == null) this.textureLocation.set(this.textureManager().register("naruto_video_dynamic", this.texture.get()));
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
        graphics.blit(textureLocation, 0, 0, 0, 0, width, height, width, height);
    }

    @Override
    public void cleanupTexture() {
        DynamicTexture dynamicTexture = this.texture.getAndSet(null);
        if (dynamicTexture != null) dynamicTexture.close();

        ResourceLocation textureLocation = this.textureLocation.getAndSet(null);
        if (textureLocation != null) this.textureManager().release(textureLocation);
    }

    private @NotNull TextureManager textureManager() {
        return Minecraft.getInstance().getTextureManager();
    }
}