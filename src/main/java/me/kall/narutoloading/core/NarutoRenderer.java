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
import org.jetbrains.annotations.Nullable;

public class NarutoRenderer {
    public static final NarutoRenderer INSTANCE = new NarutoRenderer();

    public @Nullable DynamicTexture dynamicTexture;
    public @Nullable ResourceLocation textureLocation;

    public final NarutoAudioExecutor audio = new NarutoAudioExecutor();
    public final NarutoVideoExecutor video = new NarutoVideoExecutor(this);
    public final LifetimeController lifetime = new LifetimeController(this);
    public final WindowSizeChecker windowSizeChecker = new WindowSizeChecker(this);
    public final KeyChecker keyChecker = new KeyChecker(this);

    public void setup() {
        if (!this.isEnabled()) return;
        if (this.dynamicTexture != null) return;
        this.dynamicTexture = new DynamicTexture(VideoArgs.width(), VideoArgs.height(), false);
        if (this.textureLocation == null) {
            this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
            NarutoLoading.LOGGER.info("NarutoRenderer texture location initialized: {}", this.textureLocation.toString());
        }
        this.audio.setup();
        this.video.setup();
        this.lifetime.start();
    }

    public ResourceLocation nextFrame() {
        if (!this.isEnabled()) return this.textureLocation;
        if (this.dynamicTexture == null) this.setup();
        if (this.lifetime.shouldUpdateFrame(VideoArgs.fps())) {
            NativeImage frame = this.video.fetchImage(this.lifetime.elapsedSeconds());
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

            this.keyChecker.reload();
            this.windowSizeChecker.resize();
            this.lifetime.syncSoundEngine();
            this.lifetime.lagSpikeRestart();
            this.lifetime.endRestart();
        }
    }

    public boolean isEnabled() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GenericDirtMessageScreen) return true;
        if (VideoArgs.width() == 0 || VideoArgs.height() == 0) return false;
        return minecraft.level == null && minecraft.isRunning();
    }

    public void shutdown() {
        this.audio.shutdown();
        this.video.shutdown();

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
