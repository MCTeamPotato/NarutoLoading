package me.kall.narutoloading.executor;

import me.kall.narutoloading.NarutoLoading;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NarutoAudioExecutor {
    public boolean canceled;
    public long device, context;
    public int source;
    public @Nullable ExecutorService executor;
    private @Nullable Process process;

    public void setup() {
        this.canceled = false;

        this.device = ALC10.alcOpenDevice((ByteBuffer) null);
        this.context = ALC10.alcCreateContext(this.device, (int[]) null);
        ALC10.alcMakeContextCurrent(this.context);

        ALC.createCapabilities(this.device);
        AL.createCapabilities(ALC.getCapabilities());

        this.source = AL10.alGenSources();
        AL10.alSourcef(this.source, AL10.AL_GAIN, 1.0f);

        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoAudioExecutor");
            thread.setDaemon(true);
            return thread;
        });
        this.executor.submit(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(NarutoLoading.FFMPEG_PATH, "-i", NarutoLoading.VIDEO_PATH, "-vn", "-f", "s16le", "-ac", "2", "-ar", "44100", "-loglevel", "error", "-");
                this.process = processBuilder.start();
                InputStream inputStream = this.process.getInputStream();

                byte[] buffer = new byte[4096];
                int read;

                while (!this.canceled && (read = inputStream.read(buffer)) != -1) {
                    ByteBuffer data = MemoryUtil.memAlloc(read);
                    data.put(buffer, 0, read).flip();

                    int alGenBuffers = AL10.alGenBuffers();
                    AL10.alBufferData(alGenBuffers, AL10.AL_FORMAT_STEREO16, data, 44100);
                    MemoryUtil.memFree(data);

                    AL10.alSourceQueueBuffers(this.source, alGenBuffers);

                    if (AL10.alGetSourcei(this.source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) AL10.alSourcePlay(this.source);


                    int processed = AL10.alGetSourcei(this.source, AL10.AL_BUFFERS_PROCESSED);
                    while (processed-- > 0) AL10.alDeleteBuffers(AL10.alSourceUnqueueBuffers(this.source));
                }
            } catch (Exception ignored) {}
        });
    }

    public void shutdown() {
        if (this.canceled) return;
        this.canceled = true;

        if (this.executor != null && !this.executor.isShutdown()) {
            if (this.process != null) {
                this.process.destroyForcibly();
                this.process = null;
            }
            this.executor.shutdownNow();
            this.executor = null;
        }

        if (this.source != 0) {
            AL10.alSourceStop(this.source);

            int queued = AL10.alGetSourcei(this.source, AL10.AL_BUFFERS_QUEUED);
            while (queued-- > 0) AL10.alDeleteBuffers(AL10.alSourceUnqueueBuffers(this.source));

            AL10.alDeleteSources(this.source);
            this.source = 0;
        }

        if (this.context != 0) {
            ALC10.alcDestroyContext(this.context);
            context = 0;
        }

        if (this.device != 0) {
            ALC10.alcCloseDevice(this.device);
            this.device = 0;
        }
    }
}
