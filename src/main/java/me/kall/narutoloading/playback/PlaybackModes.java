package me.kall.narutoloading.playback;

import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.media.ffmpeg.FFmpeg;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.playback.produce.util.TimeLine;
import org.jetbrains.annotations.NotNull;

public final class PlaybackModes {
    private final ClientUiTV.Buffer buffer;
    private final ClientUiTV.Image image;

    private volatile ClientUiTV<?> active;

    public PlaybackModes(@NotNull MediaRepository media, @NotNull FFmpeg ffmpeg) {
        this.buffer = new ClientUiTV.Buffer(media, ffmpeg);
        this.image = new ClientUiTV.Image(media, ffmpeg);
        this.active = NarutoConfig.compatMode() ? this.image : this.buffer;
    }

    public ClientUiTV<?> active() {
        return this.active;
    }

    public boolean isCompatMode() {
        return this.active == this.image;
    }

    public void apply() {
        ClientUiTV<?> target = NarutoConfig.compatMode() ? this.image : this.buffer;
        if (target == this.active) return;

        double seekTo = 0D;
        TimeLine timeLine = this.active.timeLine.get();
        if (timeLine != null) seekTo = timeLine.positionSec();

        ClientUiTV<?> previous = this.active;
        this.active = target;

        previous.shutdown(true);
        target.setup(seekTo);
    }
}
