package me.kall.narutoloading.noworld.core;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.LifetimeController;
import me.kall.narutoloading.noworld.core.checker.KeyChecker;
import me.kall.narutoloading.noworld.core.checker.WindowSizeChecker;
import me.kall.narutoloading.common.executor.NarutoAudioExecutor;
import me.kall.narutoloading.common.executor.NarutoVideoExecutor;
import me.kall.narutoloading.common.env.BaseEnv;
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

    public NarutoAudioExecutor audioExecutor;
    public NarutoVideoExecutor videoExecutor;

    public LifetimeController lifetime;

    public @Nullable WindowSizeChecker windowSizeChecker;
    public @Nullable KeyChecker keyChecker;

    public NarutoRenderer() {
        this.windowSizeChecker = this.runInLevel() ? null : new WindowSizeChecker(this);
        this.keyChecker = this.runInLevel() ? null : new KeyChecker(this);
    }

    public void setup() {
        if (!this.isEnabled()) return;
        this.lifetime = new LifetimeController(this, BaseEnv.noWorldVideoArgs.duration());
        this.audioExecutor = new NarutoAudioExecutor(BaseEnv.narutoConfig.video, BaseEnv.narutoConfig.audio, BaseEnv.ffmpegProvider.ffmpeg);
        this.videoExecutor = new NarutoVideoExecutor(this.lifetime, () -> BaseEnv.ffmpegProvider.ffmpeg, () -> BaseEnv.narutoConfig.widthString(), () -> BaseEnv.narutoConfig.heightString(), () -> BaseEnv.narutoConfig.video, () -> BaseEnv.narutoConfig.width(), () -> BaseEnv.narutoConfig.height(), () -> BaseEnv.noWorldVideoArgs.fps());
        if (this.dynamicTexture != null) return;
        this.dynamicTexture = new DynamicTexture(BaseEnv.narutoConfig.width(), BaseEnv.narutoConfig.height(), false);
        if (this.textureLocation == null) {
            this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
            NarutoLoading.LOGGER.info("NarutoRenderer texture location initialized: {}", this.textureLocation.toString());
        }
        this.lifetime.start();
        this.audioExecutor.setup();
        this.videoExecutor.setup();
    }

    public ResourceLocation nextFrame() {
        if (!this.isEnabled()) return this.textureLocation;
        if (this.dynamicTexture == null) this.setup();
        if (this.lifetime.shouldUpdateFrame(BaseEnv.noWorldVideoArgs.fps())) {
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

    public void renderFrame(@Nullable GuiGraphics graphics) {
        if (this.isEnabled()) {
            ResourceLocation texture = this.nextFrame();

            if (graphics != null){
                int w = graphics.guiWidth();
                int h = graphics.guiHeight();

                if (!this.runInLevel()) graphics.blit(texture, 0, 0, 0, 0, w, h, w, h);
            }


            if (this.keyChecker != null) this.keyChecker.reload();
            if (this.windowSizeChecker != null) this.windowSizeChecker.resize();
            this.lifetime.tick();
            this.lifetime.syncSoundEngine();
            this.lifetime.lagSpikeRestart();
            this.lifetime.endRestart();
        }
    }

    public boolean isEnabled() {
        if (!BaseEnv.available()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GenericDirtMessageScreen && this.runInGenericScreen()) return true;
        if (BaseEnv.narutoConfig.width() == 0 || BaseEnv.narutoConfig.height() == 0) return false;
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
