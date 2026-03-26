package me.kall.narutoloading.core.executor.audio;

import me.kall.narutoloading.core.executor.AbstractFFmpegExecutor;
import me.kall.narutoloading.data.Paths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.*;
import java.io.InputStream;
import java.util.function.Supplier;

public class EarlyAudioExecutor extends AbstractFFmpegExecutor {
    private static final AudioFormat FORMAT = new AudioFormat(44100f, 16, 2, true, false);

    private static final int BUFFER_BYTES = 8192;

    private final Supplier<String> video;
    private final Supplier<String> audio;

    private volatile @Nullable SourceDataLine line;

    private final @Nullable Supplier<Runnable> onError;

    public EarlyAudioExecutor(@Nullable Supplier<Runnable> onError, Supplier<String> video, Supplier<String> audio) {
        this.onError = onError;
        this.video = video;
        this.audio = audio;
    }

    @Override
    public void setup(String seconds) {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);

            if (!AudioSystem.isLineSupported(info)) {
                if (this.onError != null) this.onError.get().run();
                return;
            }

            SourceDataLine newLine = (SourceDataLine) AudioSystem.getLine(info);
            newLine.open(FORMAT, BUFFER_BYTES * 4);
            newLine.start();
            this.line = newLine;

        } catch (LineUnavailableException e) {
            if (this.onError != null) this.onError.get().run();
            return;
        }

        super.setup(seconds);
    }

    @Override
    protected String[] command(String seconds) {
        return new String[]{Paths.FFMPEG.toString(), "-ss", seconds, "-i",  this.audio.get().isEmpty() ? this.video.get() : this.audio.get(), "-vn", "-f",  "s16le", "-ac", "2", "-ar", "44100", "-loglevel", "error", "-"};
    }

    @Override
    protected void runLoop(@NotNull InputStream inputStream) throws Exception {
        byte[] buffer = new byte[BUFFER_BYTES];
        int read;

        while (!this.canceled && (read = inputStream.read(buffer)) != -1) {
            SourceDataLine sourceDataLine = this.line;
            if (sourceDataLine == null || !sourceDataLine.isOpen()) break;
            sourceDataLine.write(buffer, 0, read);
        }
    }

    @Override
    protected void cleanup() {
        SourceDataLine sourceDataLine = this.line;
        this.line = null;

        if (sourceDataLine != null) {
            if (this.canceled) {
                sourceDataLine.flush();
            } else {
                sourceDataLine.drain();
            }
            sourceDataLine.stop();
            sourceDataLine.close();
        }
    }
}