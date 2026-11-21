package me.kall.narutoloading.impl;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.app.Instance;
import me.kall.narutoloading.app.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.app.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.client.context.LevelStatus;
import me.kall.narutoloading.client.AbstractTV;
import me.kall.narutoloading.client.renderer.BufferFrameRenderer;
import me.kall.narutoloading.client.renderer.FrameRenderer;
import me.kall.narutoloading.produce.AbstractProducer;
import me.kall.narutoloading.produce.NarutoProducers;
import me.kall.narutoloading.produce.audio.AudioProducer;
import me.kall.narutoloading.produce.video.AbstractFrameProducer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public abstract class ClientUiTV<T> extends AbstractTV<T> {
    private static final String EARLY_SESSION = "narutoloading.early";

    protected ClientUiTV(FrameRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public boolean prerequisite() {
        if (System.getProperty(EARLY_SESSION) != null) return false;
        if (!Instance.get().media.hasSource()) return false;
        if (Instance.get().ffmpeg.ffmpeg.get() == null) return false;
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
    public @Nullable AudioProducer createAudio(@Nullable AudioArgs audioArgs) {
        return NarutoProducers.buildAudio(audioArgs);
    }

    public static final class Buffer extends ClientUiTV<ByteBuffer> {
        public Buffer() {
            super(new BufferFrameRenderer(false));
        }

        @Override
        public @NotNull AbstractFrameProducer<ByteBuffer> createVideo(@Nullable VideoArgs videoArgs) {
            return NarutoProducers.buildBuffer(videoArgs);
        }
    }

    public static final class Image extends ClientUiTV<NativeImage> {
        public Image() {
            super(new ClientUiRenderer());
        }

        @Override
        public @NotNull AbstractFrameProducer<NativeImage> createVideo(@Nullable VideoArgs videoArgs) {
            return NarutoProducers.buildImage(videoArgs);
        }
    }
}
