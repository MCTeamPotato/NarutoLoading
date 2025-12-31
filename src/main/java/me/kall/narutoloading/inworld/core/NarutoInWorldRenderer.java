package me.kall.narutoloading.inworld.core;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.LifetimeController;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.VideoArgReader;
import me.kall.narutoloading.common.executor.NarutoAudioExecutor;
import me.kall.narutoloading.common.executor.NarutoVideoExecutor;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NarutoInWorldRenderer extends NarutoRenderer {
    private @Nullable VideoArgReader videoArgReader;
    private @Nullable Runnable soundSetup;
    private @Nullable Runnable soundShutdown;

    public final InWorldScreen screen;

    public NarutoInWorldRenderer(@NotNull InWorldScreen screen) {
        this.screen = screen;
    }

    @Override
    public void setup() {
        if (!this.isEnabled()) return;

        this.videoArgReader = new VideoArgReader(this.screen.absoluteVideoPath(BaseEnv.narutoConfig.absoluteVideoPath), BaseEnv.ffmpegProvider.absoluteFFprobe);
        this.lifetime = new LifetimeController(this, this.videoArgReader.duration());

        if (!this.screen.isLocalSound()) {
            this.audioExecutor = new NarutoAudioExecutor(() -> this.screen.absoluteVideoPath(BaseEnv.narutoConfig.absoluteVideoPath), () -> this.screen.absoluteAudioPath(BaseEnv.narutoConfig.absoluteAudioPath), () -> BaseEnv.ffmpegProvider.absoluteFFmpeg);
        } else {
            this.audioExecutor = null;
            this.soundSetup = () -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null && this.screen.getLocalSound() != InWorldScreen.NO_LOCAL_SOUND) {
                    SimpleSoundInstance soundInstance = new SimpleSoundInstance(this.screen.getLocalSound(), SoundSource.MUSIC, BaseEnv.narutoConfig.volume, 1.0F, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR, this.screen.centerX(), this.screen.centerY(), this.screen.centerZ(), false);
                    Minecraft.getInstance().getSoundManager().play(soundInstance);
                    NarutoLoading.LOGGER.info("Local sound {} played at [{}, {}, {}]", this.screen.getLocalSound().toString(), this.screen.centerX(), this.screen.centerY(), this.screen.centerZ());
                }
            };
            this.soundShutdown = () -> {
                if (this.screen.getLocalSound() != InWorldScreen.NO_LOCAL_SOUND) {
                    Minecraft.getInstance().getSoundManager().stop(this.screen.getLocalSound(), SoundSource.MUSIC);
                    NarutoLoading.LOGGER.info("Local sound {} playing at [{}, {}, {}] is stopped", this.screen.getLocalSound().toString(), this.screen.centerX(), this.screen.centerY(), this.screen.centerZ());
                }
            };
        }

        this.videoExecutor = new NarutoVideoExecutor(this.lifetime, () -> BaseEnv.ffmpegProvider.absoluteFFmpeg, () -> "1280", () -> "720", () -> this.screen.absoluteVideoPath(BaseEnv.narutoConfig.absoluteVideoPath), () -> 1280, () -> 720, this.videoArgReader::fps);

        this.windowSizeChecker = null;
        this.keyChecker = null;

        if (this.dynamicTexture == null) {
            this.dynamicTexture = new DynamicTexture(1280, 720, false);
            if (this.textureLocation == null) {
                this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
                NarutoLoading.LOGGER.info("NarutoInWorldRenderer texture location initialized: {}", this.textureLocation.toString());
            }
        }

        this.lifetime.start();

        if (this.audioExecutor != null) {
            this.audioExecutor.setup();
        }

        this.videoExecutor.setup();
    }

    @Override
    public @Nullable ResourceLocation nextFrame() {
        if (!this.isEnabled()) return this.textureLocation;
        if (this.dynamicTexture == null) this.setup();
        if (this.lifetime != null && this.videoArgReader != null && this.lifetime.shouldUpdateFrame(this.videoArgReader.fps()) && this.videoExecutor != null) {
            NativeImage frame = this.videoExecutor.fetchImage(this.lifetime.elapsedSeconds());

            if (this.soundSetup != null && frame != null) {
                this.soundSetup.run();
                this.soundSetup = null;
            }

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

    @Override
    public void shutdown() {
        super.shutdown();
        if (this.soundShutdown != null) {
            this.soundShutdown.run();
            this.soundShutdown = null;
        }
    }
}
