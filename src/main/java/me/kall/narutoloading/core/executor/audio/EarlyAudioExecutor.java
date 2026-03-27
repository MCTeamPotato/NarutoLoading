package me.kall.narutoloading.core.executor.audio;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.*;
import java.io.InputStream;
import java.util.function.Supplier;

public class EarlyAudioExecutor extends AbstractAudioExecutor {
    private static final AudioFormat FORMAT = new AudioFormat(44100F, 16, 2, true, false);

    private static final int BUFFER_BYTES = 8192;

    private @Nullable SourceDataLine line;

    public EarlyAudioExecutor(@Nullable Supplier<Runnable> onError, Supplier<String> video, Supplier<String> audio) {
        super(video, audio, onError);
    }

    @Override
    public void setup(String seconds) {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);

            if (!AudioSystem.isLineSupported(info)) {
                if (this.onSoundError != null) this.onSoundError.get().run();
                return;
            }

            SourceDataLine newLine = (SourceDataLine) AudioSystem.getLine(info);
            newLine.open(FORMAT, BUFFER_BYTES * 4);
            newLine.start();
            this.line = newLine;
        } catch (LineUnavailableException e) {
            if (this.onSoundError != null) this.onSoundError.get().run();
            return;
        }

        super.setup(seconds);
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