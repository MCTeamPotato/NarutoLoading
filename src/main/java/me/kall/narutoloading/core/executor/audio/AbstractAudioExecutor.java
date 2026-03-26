package me.kall.narutoloading.core.executor.audio;

import me.kall.narutoloading.core.executor.AbstractFFmpegExecutor;
import me.kall.narutoloading.data.Paths;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class AbstractAudioExecutor extends AbstractFFmpegExecutor {
    protected final Supplier<String> video;
    protected final Supplier<String> audio;
    protected final @Nullable Supplier<Runnable> onSoundError;

    protected AbstractAudioExecutor(Supplier<String> video, Supplier<String> audio, @Nullable Supplier<Runnable> onSoundError) {
        this.video = video;
        this.audio = audio;
        this.onSoundError = onSoundError;
    }

    @Override
    protected String[] command(String seconds) {
        return new String[]{Paths.FFMPEG.toString(), "-ss", seconds, "-i",  this.audio.get().isEmpty() ? this.video.get() : this.audio.get(), "-vn", "-f",  "s16le", "-ac", "2", "-ar", "44100", "-loglevel", "error", "-"};
    }
}
