package me.kall.narutoloading.playback;

import me.kall.narutoloading.agent.bootstrap.EarlyBridge;
import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.media.ffmpeg.FFmpeg;
import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.client.context.LevelStatus;
import me.kall.narutoloading.playback.render.FrameRenderer;
import me.kall.narutoloading.playback.render.ShaderBufferRenderer;
import me.kall.narutoloading.playback.produce.AbstractProducer;
import me.kall.narutoloading.playback.produce.NarutoProducers;
import me.kall.narutoloading.playback.produce.audio.AudioProducer;
import me.kall.narutoloading.playback.produce.video.AbstractFrameProducer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public abstract class ClientUiTV<T> extends AbstractTV<T> {
    protected final MediaRepository media;
    protected final FFmpeg ffmpeg;

    protected ClientUiTV(FrameRenderer<T> renderer, @NotNull MediaRepository media, @NotNull FFmpeg ffmpeg) {
        super(renderer);
        this.media = media;
        this.ffmpeg = ffmpeg;
    }

    @Override
    public boolean prerequisite() {
        if (System.getProperty(EarlyBridge.ACTIVE_PROPERTY) != null) return false;
        if (!this.media.hasSource()) return false;
        if (this.ffmpeg.ffmpeg.get() == null) return false;
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
        return this.media.peekVideo();
    }

    @Override
    public @Nullable AudioArgs newAudioArgs() {
        return this.media.peekAudio();
    }

    @Override
    public @Nullable AudioProducer createAudio(@Nullable AudioArgs audioArgs) {
        return NarutoProducers.buildAudio(this.ffmpeg.ffmpeg.get(), audioArgs);
    }

    public static final class Buffer extends ClientUiTV<ByteBuffer> {
        public Buffer(@NotNull MediaRepository media, @NotNull FFmpeg ffmpeg) {
            super(new ShaderBufferRenderer(), media, ffmpeg);
        }

        @Override
        public @NotNull AbstractFrameProducer<ByteBuffer> createVideo(@Nullable VideoArgs videoArgs) {
            return NarutoProducers.buildBuffer(this.ffmpeg.ffmpeg.get(), videoArgs);
        }
    }
}
