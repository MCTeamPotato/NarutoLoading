package me.kall.narutoloading.core;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.base.LifetimeController;
import me.kall.narutoloading.core.base.NarutoTV;
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
    private static final NarutoRenderer INSTANCE = new NarutoRenderer();

    static {
        double absoluteRunStartNanoTime = (double) Long.parseLong(System.getProperty("narutoloading.run.start"));
        double absoluteRunEndNanoTime = (double) Long.parseLong(System.getProperty("narutoloading.run.end"));
        INSTANCE.restartAt(String.valueOf((absoluteRunEndNanoTime - absoluteRunStartNanoTime) / 1_000_000_000.0));
    }

    public static NarutoRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void createVideo() {
        Runnable onLagSpike = () -> {
            LifetimeController lifetime = this.lifetime.get();
            if (lifetime != null) lifetime.lagSpikeDetected.set(true);
        };
        NarutoVideoExecutor videoExecutor = new NarutoVideoExecutor(() -> onLagSpike, this.absoluteVideoPath(), () -> NarutoConfig.WIDTH, () -> NarutoConfig.HEIGHT, this::getFps);
        this.videoExecutor.set(videoExecutor);
        videoExecutor.setup();
    }

    @Override
    public void createAudio() {
        NarutoAudioExecutor audioExecutor = new NarutoAudioExecutor(() -> () -> RestartExecutor.schedule(() -> this.cleanup(true), () -> this.init(true), task -> Minecraft.getInstance().execute(task)), this.absoluteVideoPath(), this.absoluteAudioPath());
        this.audioExecutor.set(audioExecutor);
        audioExecutor.setup();
    }

    @Override
    public boolean isRunnable() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;

        if (screen instanceof WinScreen || screen instanceof GenericDirtMessageScreen) return true;
        if (minecraft.getOverlay() instanceof LoadingOverlay) return true;
        if (minecraft.isPaused()) return false;

        if (minecraft.level != null) {
            this.cleanup(false);
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