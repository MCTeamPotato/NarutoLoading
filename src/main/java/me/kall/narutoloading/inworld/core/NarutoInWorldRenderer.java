package me.kall.narutoloading.inworld.core;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.common.env.ffmpeg.VideoArgReader;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class NarutoInWorldRenderer extends NarutoRenderer {
    private @Nullable Runnable soundTrigger;
    private @Nullable Runnable soundShutdown;

    public final InWorldScreen screen;
    public volatile boolean pause;

    public NarutoInWorldRenderer(@NotNull InWorldScreen screen) {
        this.screen = screen;
    }

    @Override
    protected void setupSound() {
        if (this.screen.isLocalSound()) {
            this.audioExecutor = null;
            this.soundTrigger = () -> {
                LocalPlayer player = Minecraft.getInstance().player;
                ClientLevel level = Minecraft.getInstance().level;
                if (player != null && this.screen.localSound() != InWorldScreen.NO_LOCAL_SOUND && level != null) {
                    level.playSound(player, this.screen.centerX(), this.screen.centerY(), this.screen.centerZ(), new SoundEvent(this.screen.localSound()), SoundSource.BLOCKS, (float) this.soundVolume().getAsDouble(), 1.0F);
                    NarutoLoading.LOGGER.debug("{}Local sound {} played at [{}, {}, {}]", NarutoLoading.info(), this.screen.localSound().toString(), this.screen.centerX(), this.screen.centerY(), this.screen.centerZ());
                }
            };
            this.soundShutdown = () -> {
                if (this.screen.localSound() != InWorldScreen.NO_LOCAL_SOUND) {
                    Minecraft.getInstance().getSoundManager().stop(this.screen.localSound(), SoundSource.BLOCKS);
                    NarutoLoading.LOGGER.debug("{}Local sound {} playing at [{}, {}, {}] is stopped", NarutoLoading.info(), this.screen.localSound().toString(), this.screen.centerX(), this.screen.centerY(), this.screen.centerZ());
                }
            };
        } else {
            super.setupSound();
        }
    }

    @Override
    protected void readVideoArg() {
        VideoArgReader reader = new VideoArgReader(NarutoConfig.absolute(this.screen.relativeVideoPath(BaseEnv.narutoConfig.videoFileName)), BaseEnv.ffmpegProvider.absoluteFFprobe);
        this.fps = reader.fps();
        this.duration = reader.duration();
    }

    @Override
    protected DoubleSupplier soundVolume() {
        return () -> (double) this.screen.soundVolume();
    }

    @Override
    protected Supplier<String> absoluteVideoPath() {
        return () -> NarutoConfig.absolute(this.screen.relativeVideoPath(BaseEnv.narutoConfig.videoFileName));
    }

    @Override
    protected Supplier<String> absoluteAudioPath() {
        return () -> NarutoConfig.absolute(this.screen.relativeAudioPath(BaseEnv.narutoConfig.audioFileName));
    }

    @Override
    protected IntSupplier textureWidth() {
        return this.screen::videoWidth;
    }

    @Override
    protected IntSupplier textureHeight() {
        return this.screen::videoHeight;
    }

    @Override
    protected void playLocalSound() {
        if (this.soundTrigger != null) {
            this.soundTrigger.run();
            this.soundTrigger = null;
        }
    }

    @Override
    public boolean isEnabled() {
        if (this.pause) return false;
        return super.isEnabled();
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