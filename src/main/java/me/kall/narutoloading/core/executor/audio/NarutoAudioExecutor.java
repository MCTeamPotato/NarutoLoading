package me.kall.narutoloading.core.executor.audio;

import me.kall.narutoloading.data.Paths;
import me.kall.narutoloading.core.executor.AbstractFFmpegExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class NarutoAudioExecutor extends AbstractFFmpegExecutor {
    private long device, context;
    private int source;
    private boolean selfContext = false;

    private final @Nullable Supplier<Runnable> onALError;
    private final Supplier<String> video, audio;

    public NarutoAudioExecutor(@Nullable Supplier<Runnable> onALError, Supplier<String> video, Supplier<String> audio) {
        this.onALError = onALError;
        this.video = video;
        this.audio = audio;
    }

    @Override
    public void setup(String seconds) {
        this.canceled = false;

        long currentContext = ALC10.alcGetCurrentContext();

        if (currentContext == MemoryUtil.NULL) {
            this.device = ALC10.alcOpenDevice((ByteBuffer) null);
            this.context = ALC10.alcCreateContext(this.device, (int[]) null);
            ALC10.alcMakeContextCurrent(this.context);
            this.selfContext = true;
        } else {
            this.context = currentContext;
            this.device = ALC10.alcGetContextsDevice(this.context);
        }

        try {
            AL.createCapabilities(ALC.createCapabilities(this.device));
        } catch (Exception e) {
            if (this.onALError != null) this.onALError.get().run();
            this.canceled = true;
            return;
        }

        this.source = AL10.alGenSources();
        AL10.alSourcef(this.source, AL10.AL_GAIN, 1.0F);

        super.setup(seconds);
    }

    @Override
    protected String[] command(String seconds) {
        return new String[]{Paths.FFMPEG.toString(), "-ss", seconds, "-i", this.audio.get().isEmpty() ? this.video.get() : this.audio.get(), "-vn", "-f", "s16le", "-ac", "2", "-ar", "44100", "-loglevel", "error", "-"};
    }

    @Override
    protected void runLoop(@NotNull InputStream inputStream) throws Exception {
        byte[] buffer = new byte[4096];
        int read;

        while (!this.canceled && (read = inputStream.read(buffer)) != -1) {
            ByteBuffer data = MemoryUtil.memAlloc(read);
            data.put(buffer, 0, read).flip();

            int alBuffer = AL10.alGenBuffers();
            AL10.alBufferData(alBuffer, AL10.AL_FORMAT_STEREO16, data, 44100);
            MemoryUtil.memFree(data);

            AL10.alSourceQueueBuffers(this.source, alBuffer);

            if (AL10.alGetSourcei(this.source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                AL10.alSourcePlay(this.source);
            }

            int processed = AL10.alGetSourcei(this.source, AL10.AL_BUFFERS_PROCESSED);
            while (processed-- > 0) {
                AL10.alDeleteBuffers(AL10.alSourceUnqueueBuffers(this.source));
            }
        }
    }

    @Override
    protected void cleanup() {
        if (this.source != 0) {
            AL10.alSourceStop(this.source);

            int queued = AL10.alGetSourcei(this.source, AL10.AL_BUFFERS_QUEUED);
            while (queued-- > 0) {
                AL10.alDeleteBuffers(AL10.alSourceUnqueueBuffers(this.source));
            }

            AL10.alDeleteSources(this.source);
            this.source = 0;
        }

        if (this.selfContext) {
            this.selfContext = false;

            if (this.context != 0) {
                ALC10.alcDestroyContext(this.context);
                this.context = 0;
            }

            if (this.device != 0) {
                ALC10.alcCloseDevice(this.device);
                this.device = 0;
            }
        }
    }
}
