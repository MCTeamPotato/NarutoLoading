package me.kall.narutoloading.inworld.core;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.LifetimeController;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.common.env.ffmpeg.VideoArgReader;
import me.kall.narutoloading.common.executor.NarutoAudioExecutor;
import me.kall.narutoloading.common.executor.NarutoVideoExecutor;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NarutoInWorldRenderer extends NarutoRenderer {
    private @Nullable Runnable soundSetup;
    private @Nullable Runnable soundShutdown;

    public final InWorldScreen screen;

    public NarutoInWorldRenderer(@NotNull InWorldScreen screen) {
        this.screen = screen;
    }

    @Override
    public void setup() {
        if (!this.isEnabled()) return;
        long absoluteSetupTime = System.nanoTime();

        VideoArgReader videoArgReader = new VideoArgReader(NarutoConfig.absolute(this.screen.relativeVideoPath(BaseEnv.narutoConfig.videoFileName)), BaseEnv.ffmpegProvider.absoluteFFprobe);
        if (!this.screen.isLocalSound()) {
            this.audioExecutor = new NarutoAudioExecutor(() -> NarutoConfig.absolute(this.screen.relativeVideoPath(BaseEnv.narutoConfig.videoFileName)), () -> NarutoConfig.absolute(this.screen.relativeAudioPath(BaseEnv.narutoConfig.audioFileName)), () -> BaseEnv.ffmpegProvider.absoluteFFmpeg);
        } else {
            this.audioExecutor = null;
            this.soundSetup = () -> {
                LocalPlayer player = Minecraft.getInstance().player;
                ClientLevel level = Minecraft.getInstance().level;
                if (player != null && this.screen.getLocalSound() != InWorldScreen.NO_LOCAL_SOUND && level != null) {
                    Holder<SoundEvent> soundEvent = Holder.direct(SoundEvent.createVariableRangeEvent(screen.getLocalSound()));
                    level.playSeededSound(player, screen.centerX(), screen.centerY(), screen.centerZ(), soundEvent, SoundSource.BLOCKS, 4.0F, 1.0F, level.random.nextLong());
                    NarutoLoading.LOGGER.info("{}Local sound {} played at [{}, {}, {}]", NarutoLoading.info(), this.screen.getLocalSound().toString(), this.screen.centerX(), this.screen.centerY(), this.screen.centerZ());
                }
            };
            this.soundShutdown = () -> {
                if (this.screen.getLocalSound() != InWorldScreen.NO_LOCAL_SOUND) {
                    Minecraft.getInstance().getSoundManager().stop(this.screen.getLocalSound(), SoundSource.BLOCKS);
                    NarutoLoading.LOGGER.info("{}Local sound {} playing at [{}, {}, {}] is stopped", NarutoLoading.info(), this.screen.getLocalSound().toString(), this.screen.centerX(), this.screen.centerY(), this.screen.centerZ());
                }
            };
        }

        this.lifetime = new LifetimeController(this, videoArgReader.duration(), absoluteSetupTime);
        this.videoExecutor = new NarutoVideoExecutor(this.lifetime, () -> BaseEnv.ffmpegProvider.absoluteFFmpeg, () -> "1280", () -> "720", () -> NarutoConfig.absolute(this.screen.relativeVideoPath(BaseEnv.narutoConfig.videoFileName)), () -> 1280, () -> 720, videoArgReader::fps);

        if (this.dynamicTexture == null) {
            this.dynamicTexture = new DynamicTexture(1280, 720, false);
            if (this.textureLocation == null) {
                this.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.dynamicTexture);
                NarutoLoading.LOGGER.info("{}NarutoInWorldRenderer texture location initialized: {}", NarutoLoading.info(), this.textureLocation.toString());
            }
        }

        this.lifetime.start();

        this.videoExecutor.setup();
        if (this.audioExecutor != null) this.audioExecutor.setup();
    }

    @Override
    protected void setupLocalSound() {
        if (this.soundSetup != null) {
            this.soundSetup.run();
            this.soundSetup = null;
        }
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
