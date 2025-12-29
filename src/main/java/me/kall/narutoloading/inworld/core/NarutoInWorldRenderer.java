package me.kall.narutoloading.inworld.core;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.LifetimeController;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.executor.NarutoAudioExecutor;
import me.kall.narutoloading.common.executor.NarutoVideoExecutor;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import me.kall.narutoloading.common.env.VideoArgReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class NarutoInWorldRenderer extends NarutoRenderer {
    private VideoArgReader videoArgReader;
    private final InWorldScreen screen;

    public NarutoInWorldRenderer(@NotNull InWorldScreen screen) {
        this.screen = screen;
    }

    @Override
    public void setup() {
        if (!this.isEnabled()) return;
        this.videoArgReader = new VideoArgReader(this.screen.video(BaseEnv.narutoConfig.absoluteVideoPath), BaseEnv.ffmpegProvider.absoluteFFprobe);
        this.lifetime = new LifetimeController(this, this.videoArgReader.duration());

        this.audioExecutor = new NarutoAudioExecutor(this.screen.video(BaseEnv.narutoConfig.absoluteVideoPath), this.screen.audio(BaseEnv.narutoConfig.absoluteAudioPath), BaseEnv.ffmpegProvider.absoluteFFmpeg);
        this.videoExecutor = new NarutoVideoExecutor(this.lifetime, () -> BaseEnv.ffmpegProvider.absoluteFFmpeg, () -> "1280", () -> "720", () -> this.screen.video(BaseEnv.narutoConfig.absoluteVideoPath), () -> 1280, () -> 720, this.videoArgReader::fps);

        this.windowSizeChecker = null;
        this.keyChecker = null;
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
    public boolean runInLevel() {
        return true;
    }

    @Override
    public boolean runInGenericScreen() {
        return false;
    }
}
