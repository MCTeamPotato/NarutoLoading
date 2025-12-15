package me.kall.narutoloading.core;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.core.detection.KeyChecker;
import me.kall.narutoloading.core.detection.WindowSizeChecker;
import me.kall.narutoloading.data.VideoArgs;
import me.kall.narutoloading.executor.NarutoAudioExecutor;
import me.kall.narutoloading.executor.NarutoVideoExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class NarutoRenderer {
    public static final NarutoRenderer INSTANCE = new NarutoRenderer();

    public @Nullable DynamicTexture dynamicTexture;
    public @Nullable ResourceLocation textureLocation;

    public final LifetimeController lifetime = new LifetimeController();

    public void setup() {
        if (this.dynamicTexture != null) return;
        this.dynamicTexture = new DynamicTexture(VideoArgs.width(), VideoArgs.height(), false);
        if (this.textureLocation == null) this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
        NarutoAudioExecutor.INSTANCE.setup();
        NarutoVideoExecutor.INSTANCE.setup();
        this.lifetime.start();
    }

    private ResourceLocation nextFrame() {
        if (this.dynamicTexture == null) this.setup();
        if (this.lifetime.shouldUpdateFrame(VideoArgs.fps())) {
            NativeImage frame = NarutoVideoExecutor.INSTANCE.fetchImage(this.lifetime.elapsedSeconds());
            if (frame != null) {
                this.dynamicTexture.setPixels(frame);
                this.dynamicTexture.upload();
                frame.close();
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

        this.textureLocation = null;

        this.lifetime.stop();
    }
}
