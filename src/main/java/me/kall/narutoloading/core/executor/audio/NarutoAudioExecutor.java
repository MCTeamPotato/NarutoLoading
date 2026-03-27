package me.kall.narutoloading.core.executor.audio;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class NarutoAudioExecutor extends AbstractAudioExecutor {
    private final AtomicLong device = new AtomicLong(0L);
    private final AtomicLong context = new AtomicLong(0L);
    private final AtomicInteger source = new AtomicInteger(0);
    private final AtomicBoolean selfContext = new AtomicBoolean(false);

    public NarutoAudioExecutor(@Nullable Supplier<Runnable> onALError, Supplier<String> video, Supplier<String> audio) {
        super(video, audio, onALError);
    }

    @Override
    public void setup(String seconds) {
        long currentContext = ALC10.alcGetCurrentContext();
        if (currentContext == MemoryUtil.NULL) {
            long device = ALC10.alcOpenDevice((ByteBuffer) null);
            long context = ALC10.alcCreateContext(device, (int[]) null);
            ALC10.alcMakeContextCurrent(context);
            this.device.set(device);
            this.context.set(context);
            this.selfContext.set(true);
        } else {
            this.context.set(currentContext);
            this.device.set(ALC10.alcGetContextsDevice(currentContext));
        }

        try {
            AL.createCapabilities(ALC.createCapabilities(this.device.get()));
        } catch (Exception e) {
            if (this.onSoundError != null) this.onSoundError.get().run();
            this.canceled.set(true);
            return;
        }

        if (this.canceled.get()) {
            this.cleanup();
            return;
        }

        int sources = AL10.alGenSources();
        this.source.set(sources);
        AL10.alSourcef(sources, AL10.AL_GAIN, 1.0F);

        super.setup(seconds);
    }

    @Override
    protected void runLoop(@NotNull InputStream inputStream) throws Exception {
        byte[] buffer = new byte[4096];
        int read;

        while (!this.canceled.get() && (read = inputStream.read(buffer)) != -1) {
            ByteBuffer data = MemoryUtil.memAlloc(read);
            data.put(buffer, 0, read).flip();

            int alBuffer = AL10.alGenBuffers();
            AL10.alBufferData(alBuffer, AL10.AL_FORMAT_STEREO16, data, 44100);
            MemoryUtil.memFree(data);

            int source = this.source.get();
            AL10.alSourceQueueBuffers(source, alBuffer);

            if (AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                AL10.alSourcePlay(source);
            }

            int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
            while (processed-- > 0) {
                AL10.alDeleteBuffers(AL10.alSourceUnqueueBuffers(source));
            }
        }
    }

    @Override
    protected void cleanup() {
        int source = this.source.getAndSet(0);
        if (source != 0) {
            AL10.alSourceStop(source);
            int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
            while (queued-- > 0) {
                AL10.alDeleteBuffers(AL10.alSourceUnqueueBuffers(source));
            }
            AL10.alDeleteSources(source);
        }

        if (this.selfContext.compareAndSet(true, false)) {
            long context = this.context.getAndSet(0L);
            if (context != 0L) ALC10.alcDestroyContext(context);

            long device = this.device.getAndSet(0L);
            if (device != 0L) ALC10.alcCloseDevice(device);
        }
    }
}