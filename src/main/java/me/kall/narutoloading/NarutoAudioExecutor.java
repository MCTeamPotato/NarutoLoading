package me.kall.narutoloading;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NarutoAudioExecutor {
    public static boolean canceled;
    public static long device, context;
    public static int source;
    public static ExecutorService executor;

    public static void setup() {
        canceled = false;

        device = ALC10.alcOpenDevice((ByteBuffer) null);
        context = ALC10.alcCreateContext(device, (int[]) null);
        ALC10.alcMakeContextCurrent(context);

        ALC.createCapabilities(device);
        AL.createCapabilities(ALC.getCapabilities());

        source = AL10.alGenSources();
        AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);

        executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoAudioExecutor");
            thread.setDaemon(true);
            return thread;
        });
        executor.submit(() -> {
            Process process = null;

            try {
                ProcessBuilder processBuilder = new ProcessBuilder(NarutoLoading.FFMPEG_PATH, "-i", NarutoLoading.VIDEO_PATH, "-vn", "-f", "s16le", "-ac", "2", "-ar", "44100", "-loglevel", "error", "-");
                process = processBuilder.start();
                InputStream inputStream = process.getInputStream();

                byte[] buffer = new byte[4096];
                int read;

                while (!canceled && (read = inputStream.read(buffer)) != -1) {
                    ByteBuffer data = MemoryUtil.memAlloc(read);
                    data.put(buffer, 0, read).flip();

                    int alGenBuffers = AL10.alGenBuffers();
                    AL10.alBufferData(alGenBuffers, AL10.AL_FORMAT_STEREO16, data, 44100);
                    MemoryUtil.memFree(data);

                    AL10.alSourceQueueBuffers(source, alGenBuffers);

                    int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                    if (state != AL10.AL_PLAYING) {
                        AL10.alSourcePlay(source);
                    }

                    int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
                    while (processed-- > 0) {
                        AL10.alDeleteBuffers(AL10.alSourceUnqueueBuffers(source));
                    }
                }
            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            } finally {
                if (process != null) process.destroyForcibly();
            }
        });
    }

    public static void shutdown() {
        canceled = true;

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }

        if (source != 0) {
            AL10.alSourceStop(source);

            int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
            while (queued-- > 0) {
                AL10.alDeleteBuffers(AL10.alSourceUnqueueBuffers(source));
            }

            AL10.alDeleteSources(source);
            source = 0;
        }

        if (context != 0) {
            ALC10.alcDestroyContext(context);
            context = 0;
        }

        if (device != 0) {
            ALC10.alcCloseDevice(device);
            device = 0;
        }
    }
}
