package me.kall.narutoloading.noworld.core;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.LifetimeController;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.ffmpeg.VideoArgReader;
import me.kall.narutoloading.common.executor.NarutoAudioExecutor;
import me.kall.narutoloading.common.executor.NarutoVideoExecutor;
import me.kall.narutoloading.noworld.core.checker.KeyChecker;
import me.kall.narutoloading.noworld.core.checker.WindowSizeChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class NarutoRenderer {
    public static final NarutoRenderer INSTANCE = new NarutoRenderer();

    public @Nullable DynamicTexture dynamicTexture;
    public @Nullable ResourceLocation textureLocation;

    public @Nullable NarutoAudioExecutor audioExecutor;
    public @Nullable NarutoVideoExecutor videoExecutor;

    public @Nullable LifetimeController lifetime;

    public @Nullable WindowSizeChecker windowSizeChecker;
    public @Nullable KeyChecker keyChecker;

    protected double fps;
    protected long duration;

    public NarutoRenderer() {
        this.windowSizeChecker = this.runInLevel() ? null : new WindowSizeChecker(this);
        this.keyChecker = this.runInLevel() ? null : new KeyChecker(this);
    }

    public void setup() {
        if (!this.isEnabled()) return;
        long absoluteSetupTime = System.nanoTime();

        this.readVideoArg();
        this.lifetime = new LifetimeController(this, this.duration, absoluteSetupTime);
        this.videoExecutor = new NarutoVideoExecutor(this.lifetime, () -> BaseEnv.ffmpegProvider.absoluteFFmpeg, this.absoluteVideoPath(), this.textureWidth(), this.textureHeight(), () -> this.fps);

        this.setupSound();
        this.setupTexture();
        this.videoExecutor.setup();
        if (this.audioExecutor != null) {
            this.audioExecutor.setup();
        } else {
            this.playLocalSound();
        }
        this.lifetime.start();
    }

    protected void readVideoArg() {
        VideoArgReader reader = new VideoArgReader(BaseEnv.narutoConfig.absoluteVideoPath, BaseEnv.ffmpegProvider.absoluteFFprobe);
        this.fps = reader.fps();
        this.duration = reader.duration();
    }

    protected void setupSound() {
        this.audioExecutor = new NarutoAudioExecutor(this.absoluteVideoPath(), this.absoluteAudioPath(), () -> BaseEnv.ffmpegProvider.absoluteFFmpeg, this.soundVolume());
    }

    protected void setupTexture() {
        if (this.dynamicTexture != null) return;
        this.dynamicTexture = new DynamicTexture(this.textureWidth().getAsInt(), this.textureHeight().getAsInt(), false);
        if (this.textureLocation == null) {
            this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
            NarutoLoading.LOGGER.info("{}NarutoRenderer texture location initialized: {}", NarutoLoading.info(), this.textureLocation.toString());
        }
    }

    protected DoubleSupplier soundVolume() {
        return () -> BaseEnv.narutoConfig.volume;
    }

    protected Supplier<String> absoluteVideoPath() {
        return () -> BaseEnv.narutoConfig.absoluteVideoPath;
    }

    protected Supplier<String> absoluteAudioPath() {
        return () -> BaseEnv.narutoConfig.absoluteAudioPath;
    }

    protected IntSupplier textureWidth() {
        return () -> BaseEnv.narutoConfig.width();
    }

    protected IntSupplier textureHeight() {
        return () -> BaseEnv.narutoConfig.height();
    }

    public ResourceLocation nextFrame() {
        if (!this.isEnabled()) return this.textureLocation;
        if (this.dynamicTexture == null) this.setup();
        if (this.lifetime != null && this.lifetime.shouldUpdateFrame(this.fps) && this.videoExecutor != null) {
            NativeImage frame = this.videoExecutor.fetchImage(this.lifetime.elapsedSeconds());
            if (frame != null) {
                this.dynamicTexture.setPixels(frame);
                this.dynamicTexture.upload();
                frame.close();
            }
        }
        return this.textureLocation;
    }

    protected void playLocalSound() {}

    public boolean isRunning() {
        return this.lifetime != null && this.lifetime.isRunning();
    }

    public void renderFrame(@Nullable GuiGraphics graphics) {
        if (this.isEnabled()) {

            if (this.lifetime != null) {
                this.lifetime.syncSoundEngine();
                this.lifetime.lagSpikeRestart();
                this.lifetime.endRestart();
            }

            ResourceLocation texture = this.nextFrame();
            if (texture == null) return;

            if (graphics != null){
                int w = graphics.guiWidth();
                int h = graphics.guiHeight();

                if (!this.runInLevel()) graphics.blit(texture, 0, 0, 0, 0, w, h, w, h);
            }

            if (this.keyChecker != null) this.keyChecker.reload();
            if (this.windowSizeChecker != null) this.windowSizeChecker.resize();
        }
    }

    public boolean isEnabled() {
        if (!BaseEnv.available()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (BaseEnv.narutoConfig.width() == 0 || BaseEnv.narutoConfig.height() == 0) return false;
        if (minecraft.screen instanceof GenericDirtMessageScreen && this.runInGenericScreen()) return true;
        if (minecraft.isPaused()) return false;
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
        return Minecraft.getInstance().level != null;
    }

    public boolean runInLevel() {
        return false;
    }

    public boolean runInGenericScreen() {
        return true;
    }

    public void shutdown() {
        if (this.audioExecutor != null) {
            this.audioExecutor.shutdown();
            this.audioExecutor = null;
        }

        if (this.videoExecutor != null) {
            this.videoExecutor.shutdown();
            this.videoExecutor = null;
        }

        if (this.lifetime != null) {
            this.lifetime.stop();
            this.lifetime = null;
        }

        if (this.dynamicTexture != null) {
            this.dynamicTexture.close();
            this.dynamicTexture = null;
        }

        if (this.textureLocation != null) {
            Minecraft.getInstance().getTextureManager().release(this.textureLocation);
            this.textureLocation = null;
        }
    }
}
