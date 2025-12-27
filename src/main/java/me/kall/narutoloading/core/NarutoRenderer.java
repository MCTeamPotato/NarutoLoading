package me.kall.narutoloading.core;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.detection.KeyChecker;
import me.kall.narutoloading.core.detection.WindowSizeChecker;
import me.kall.narutoloading.core.execution.NarutoAudioExecutor;
import me.kall.narutoloading.core.execution.NarutoVideoExecutor;
import me.kall.narutoloading.data.FFmpegProvider;
import me.kall.narutoloading.data.NarutoConfig;
import me.kall.narutoloading.data.SourceRoller;
import me.kall.narutoloading.data.VideoArgReader;
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

    public final NarutoConfig narutoConfig;
    public final NarutoAudioExecutor audioExecutor;
    public final NarutoVideoExecutor videoExecutor;
    public final LifetimeController lifetime;
    public final WindowSizeChecker windowSizeChecker;
    public final KeyChecker keyChecker;
    public final VideoArgReader videoArgReader;
    public final FFmpegProvider ffmpegProvider;
    public final SourceRoller sourceRoller;

    public NarutoRenderer() {
        this.narutoConfig = new NarutoConfig();
        this.sourceRoller = new SourceRoller(this);
        this.ffmpegProvider = new FFmpegProvider(this);
        this.videoArgReader = new VideoArgReader(this);
        this.audioExecutor = new NarutoAudioExecutor(this);
        this.videoExecutor = new NarutoVideoExecutor(this);
        this.lifetime = new LifetimeController(this);
        this.windowSizeChecker = new WindowSizeChecker(this);
        this.keyChecker = new KeyChecker(this);
    }

    public void setup() {
        if (!this.isEnabled()) return;
        this.narutoConfig.init();
        this.ffmpegProvider.init();
        if (this.dynamicTexture != null) return;
        this.dynamicTexture = new DynamicTexture(this.videoArgReader.width(), this.videoArgReader.height(), false);
        if (this.textureLocation == null) {
            this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
            NarutoLoading.LOGGER.info("NarutoRenderer texture location initialized: {}", this.textureLocation.toString());
        }
        this.audioExecutor.setup();
        this.videoExecutor.setup();
        this.lifetime.start();
    }

    public ResourceLocation nextFrame() {
        if (!this.isEnabled()) {
            return this.textureLocation;
        }
        if (this.dynamicTexture == null) this.setup();
        if (this.lifetime.shouldUpdateFrame(this.videoArgReader.fps())) {
            NativeImage frame = this.videoExecutor.fetchImage(this.lifetime.elapsedSeconds());
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
        if (minecraft.screen instanceof GenericDirtMessageScreen && this.runInGenericScreen()) return true;
        if (this.videoArgReader.width() == 0 || this.videoArgReader.height() == 0) return false;
        if (this.runInLevel()) {
            if (!this.hasLevel()) {
                this.shutdown();
                return false;
            }
        } else {
            if (this.hasLevel()) {
                this.shutdown();
                return false;
            }
        }
        return minecraft.isRunning();
    }

    public boolean hasLevel() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null;
    }

    public boolean runInLevel() {
        return false;
    }

    public boolean runInGenericScreen() {
        return true;
    }

    public void shutdown() {
        this.audioExecutor.shutdown();
        this.videoExecutor.shutdown();

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
