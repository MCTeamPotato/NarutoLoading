package me.kall.narutoloading.wall.client;

import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.playback.AbstractTV;
import me.kall.narutoloading.playback.produce.NarutoProducers;
import me.kall.narutoloading.playback.produce.audio.AudioProducer;
import me.kall.narutoloading.playback.produce.video.AbstractFrameProducer;
import me.kall.narutoloading.playback.render.FrameRenderer;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.client.render.WallBufferRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public abstract class WallTV<T> extends AbstractTV<T> {
    public enum State { DORMANT, ACTIVE }

    public final Wall wall;

    protected final MediaRepository media;

    State state = State.DORMANT;
    long lastVisibleNanos;

    boolean restartOnActivate;
    boolean audioSelected;
    long audioChangedNanos;

    protected WallTV(@NotNull FrameRenderer<T> renderer, @NotNull Wall wall, @NotNull MediaRepository media) {
        super(renderer);
        this.wall = wall;
        this.media = media;
    }

    public boolean isActive() {
        return this.state == State.ACTIVE;
    }

    @Override
    public boolean prerequisite() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.dimension().location().equals(this.wall.dimension)) return true;

        if (this.isRunning()) this.shutdown(true);
        return false;
    }

    @Override
    public @Nullable VideoArgs newVideoArgs() {
        MediaRepository.Media media = this.wall.media.isBlank() ? this.media.current() : this.media.find(this.wall.media);
        return media == null ? null : media.video();
    }

    @Override
    public @Nullable AudioArgs newAudioArgs() {
        if (!this.audioSelected || !NarutoConfig.worldAudioEnabled()) return null;

        MediaRepository.Media media = this.wall.media.isBlank() ? this.media.current() : this.media.find(this.wall.media);
        return media == null ? null : media.audio();
    }

    @Override
    public @Nullable AudioProducer createAudio(@Nullable AudioArgs audioArgs) {
        return NarutoProducers.buildAudio(NarutoContext.get().tools().ffmpeg.ffmpeg.get(), audioArgs);
    }

    public static final class Buffer extends WallTV<ByteBuffer> {
        public Buffer(@NotNull Wall wall, @NotNull MediaRepository media) {
            super(new WallBufferRenderer(wall), wall, media);
        }

        @Override
        public @NotNull AbstractFrameProducer<ByteBuffer> createVideo(@Nullable VideoArgs videoArgs) {
            return NarutoProducers.buildWorldBuffer(NarutoContext.get().tools().ffmpeg.ffmpeg.get(), videoArgs);
        }
    }
}
