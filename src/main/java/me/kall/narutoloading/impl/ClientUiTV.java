package me.kall.narutoloading.impl;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.app.Instance;
import me.kall.narutoloading.app.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.app.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.client.context.LevelStatus;
import me.kall.narutoloading.client.core.AbstractTV;
import me.kall.narutoloading.produce.AbstractProducer;
import me.kall.narutoloading.produce.NarutoProducers;
import me.kall.narutoloading.produce.audio.AudioProducer;
import me.kall.narutoloading.produce.video.AbstractFrameProducer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import org.jetbrains.annotations.Nullable;

public class ClientUiTV extends AbstractTV<NativeImage> {
    public static final ClientUiTV INSTANCE = new ClientUiTV();

    public ClientUiTV() {
        super(new ClientUiRenderer());
    }

    @Override
    public boolean prerequisite() {
        if (!Instance.get().media.hasSource()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen instanceof WinScreen || screen instanceof GenericDirtMessageScreen) return true;
        if (LevelStatus.notFound()) {
            return minecraft.isRunning();
        } else {
            this.shutdown(true);
            return false;
        }
    }

    public boolean hasVideo() {
        AbstractProducer video = this.video.get();
        return video != null && !video.off.get();
    }

    public boolean hasAudio() {
        AbstractProducer audio = this.audio.get();
        return audio != null && !audio.off.get();
    }

    @Override
    public @Nullable VideoArgs newVideoArgs() {
        return Instance.get().media.peekVideo();
    }

    @Override
    public @Nullable AudioArgs newAudioArgs() {
        return Instance.get().media.peekAudio();
    }

    @Override
    public @Nullable AbstractFrameProducer<NativeImage> createVideo(@Nullable VideoArgs videoArgs) {
        // Without FFmpeg the producer could never start; returning null keeps the TV idle so the
        // next tick retries and playback begins as soon as the user configures it.
        if (Instance.get().ffmpeg.ffmpeg.get() == null) return null;
        return NarutoProducers.buildImage(videoArgs);
    }

    @Override
    public @Nullable AudioProducer createAudio(@Nullable AudioArgs audioArgs) {
        if (Instance.get().ffmpeg.ffmpeg.get() == null) return null;
        return NarutoProducers.buildAudio(audioArgs);
    }
}
