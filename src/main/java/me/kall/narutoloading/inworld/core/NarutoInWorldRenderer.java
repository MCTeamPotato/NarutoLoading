package me.kall.narutoloading.inworld.core;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.LifetimeController;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.executor.NarutoAudioExecutor;
import me.kall.narutoloading.common.executor.NarutoVideoExecutor;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import me.kall.narutoloading.util.VideoArgReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class NarutoInWorldRenderer extends NarutoRenderer {
    private final InWorldScreen screen;
    private final VideoArgReader videoArgReader;

    public NarutoInWorldRenderer(@NotNull InWorldScreen screen) {
        this.lifetime = new LifetimeController(this);

        this.audioExecutor = new NarutoAudioExecutor(screen.video, screen.audio, BaseEnv.ffmpegProvider.ffmpeg);
        this.videoArgReader = new VideoArgReader(screen.video, BaseEnv.ffmpegProvider.ffprobe);
        this.videoExecutor = new NarutoVideoExecutor(this.lifetime, () -> BaseEnv.ffmpegProvider.ffmpeg, () -> "1280", () -> "720", () -> screen.video, () -> 1280, () -> 720, videoArgReader::fps);

        this.windowSizeChecker = null;
        this.keyChecker = null;
        this.screen = screen;
    }

    @Override
    public void setup() {
        if (!this.isEnabled()) return;
        if (this.dynamicTexture != null) return;
        this.dynamicTexture = new DynamicTexture(1280, 720, false);
        if (this.textureLocation == null) {
            this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
            NarutoLoading.LOGGER.info("NarutoInWorldRenderer texture location initialized: {}", this.textureLocation.toString());
        }
        this.lifetime.start();
        this.audioExecutor.setup();
        this.videoExecutor.setup();
    }

    @Override
    public ResourceLocation nextFrame() {
        if (!this.isEnabled()) return this.textureLocation;
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

    @Override
    public boolean isEnabled() {
        return !this.screen.video.isBlank() && !this.screen.audio.isBlank() && super.isEnabled();
    }

    @Override
    public boolean runInLevel() {
        return true;
    }

    @Override
    public boolean runInGenericScreen() {
        return false;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.screen.video = "";
        this.screen.audio = "";
    }
}
