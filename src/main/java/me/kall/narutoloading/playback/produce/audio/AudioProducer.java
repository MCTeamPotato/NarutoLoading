package me.kall.narutoloading.playback.produce.audio;

import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.playback.produce.AbstractProducer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.DoubleConsumer;

public final class AudioProducer extends AbstractProducer {
    private static final double CHUNK_SECOND = 0.1D;

    private final AtomicInteger volume;
    private final AtomicBoolean volumeChanged = new AtomicBoolean(true);

    private final String ffmpeg;

    public final AudioArgs args;

    private final int chunkSamples, preRoll;

    private final int openALFormat;

    private final long chunkNanos;

    public AudioProducer(String ffmpeg, float volume, @NotNull AudioArgs args, double bufferSeconds) {
        this.ffmpeg = ffmpeg;

        this.volume = new AtomicInteger(Float.floatToIntBits(volume));
        this.args = args;

        this.openALFormat = args.channels() == 1 ? AL11.AL_FORMAT_MONO16 : AL11.AL_FORMAT_STEREO16;

        this.chunkSamples = Math.max(1, (int) Math.round(args.sampleRate() * CHUNK_SECOND));
        this.preRoll = Math.max(1, (int) Math.round(bufferSeconds / CHUNK_SECOND));
        this.chunkNanos = (long) ((double) this.chunkSamples / args.sampleRate() * 1_000_000_000D);
    }

    public float volume() {
        return Float.intBitsToFloat(this.volume.get());
    }

    @Override
    public AudioProducer withWarmListener(Runnable warmListener) {
        super.withWarmListener(warmListener);
        return this;
    }

    public void volume(float volume) {
        if (this.volume() != volume) {
            this.volume.set(Float.floatToIntBits(volume));
            this.volumeChanged.set(true);
        }
    }

    @Override
    protected String @Nullable [] setCommand(double seekTo) {
        if (this.ffmpeg == null) return null;
        return new String[]{this.ffmpeg, "-ss", String.valueOf(seekTo + this.args.startSec()), "-i", this.args.absPath(), "-vn", "-f", "s16le", "-ac", String.valueOf(this.args.channels()), "-ar", String.valueOf(this.args.sampleRate()), "-loglevel", "error", "-"};
    }

    @Override
    protected void forInput(InputStream input) throws IOException {
        long device = 0;
        long context = 0;
        int source = 0;
        ByteBuffer direct = null;

        try {
            device = ALC11.alcOpenDevice((ByteBuffer) null);
            context = ALC11.alcCreateContext(device, (IntBuffer) null);
            EXTThreadLocalContext.alcSetThreadContext(context);
            AL.createCapabilities(ALC.createCapabilities(device));

            source = AL11.alGenSources();
            AL11.alSourcef(source, AL11.AL_GAIN, this.volume());
            this.volumeChanged.set(false);

            byte[] chunk = new byte[this.args.channels() * 2 * this.chunkSamples];
            direct = MemoryUtil.memAlloc(chunk.length);

            for (int index = 0; index < this.preRoll && !this.off.get(); index++) {
                int read = this.readFully(input, chunk);
                if (read <= 0) break;
                this.queue(source, direct, chunk, read);
            }

            if (!this.awaitEager()) return;

            this.notifyWarmListener();

            this.ready.set(true);
            AL11.alSourcePlay(source);

            boolean ended = false;

            while (!this.off.get()) {
                int processed = AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED);
                while (processed-- > 0) AL11.alDeleteBuffers(AL11.alSourceUnqueueBuffers(source));

                while (!this.off.get() && AL11.alGetSourcei(source, AL11.AL_BUFFERS_QUEUED) < this.preRoll) {
                    int read = this.readFully(input, chunk);
                    if (read <= 0) {
                        ended = true;
                        break;
                    }
                    this.queue(source, direct, chunk, read);
                }

                if (ended) break;

                if (AL11.alGetSourcei(source, AL11.AL_SOURCE_STATE) != AL11.AL_PLAYING) AL11.alSourcePlay(source);
                if (this.volumeChanged.compareAndSet(true, false)) AL11.alSourcef(source, AL11.AL_GAIN, this.volume());

                LockSupport.parkNanos(this.chunkNanos);
                if (Thread.interrupted()) break;
            }

            while (!this.off.get() && AL11.alGetSourcei(source, AL11.AL_BUFFERS_QUEUED) > 0) {
                int processed = AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED);
                while (processed-- > 0) AL11.alDeleteBuffers(AL11.alSourceUnqueueBuffers(source));

                LockSupport.parkNanos(this.chunkNanos);
                if (Thread.interrupted()) return;
            }
        } finally {
            if (source != 0) {
                AL11.alSourceStop(source);
                int queued = AL11.alGetSourcei(source, AL11.AL_BUFFERS_QUEUED);
                while (queued-- > 0) AL11.alDeleteBuffers(AL11.alSourceUnqueueBuffers(source));
                AL11.alDeleteSources(source);
            }

            if (context != 0) {
                EXTThreadLocalContext.alcSetThreadContext(0);
                ALC11.alcDestroyContext(context);
            }

            if (device != 0) ALC11.alcCloseDevice(device);

            if (direct != null) MemoryUtil.memFree(direct);
        }
    }

    private void queue(int source, @NotNull ByteBuffer direct, byte[] chunk, int read) {
        direct.clear();
        direct.put(chunk, 0, read).flip();

        int buffer = AL11.alGenBuffers();
        AL11.alBufferData(buffer, this.openALFormat, direct, this.args.sampleRate());
        AL11.alSourceQueueBuffers(source, buffer);
    }

    private int readFully(InputStream input, byte @NotNull [] chunk) throws IOException {
        int total = 0;

        while (total < chunk.length) {
            int read = input.read(chunk, total, chunk.length - total);
            if (read == -1) break;
            total += read;
        }

        return total - total % (this.args.channels() * 2);
    }
}
