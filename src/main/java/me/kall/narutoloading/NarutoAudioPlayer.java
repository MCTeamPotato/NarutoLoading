package me.kall.narutoloading;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NarutoAudioPlayer {

    private static final String FFMPEG_PATH = "D:\\ffmpeg\\bin\\ffmpeg.exe";
    private static final String VIDEO_PATH = "config/naruto.mp4";

    private static volatile boolean cancelled = false;
    private static long device;
    private static long context;
    private static int source;
    private static ExecutorService executor;

    public static void init() {
        if (source != 0) return;

        initOpenAL();
        startAsyncDecode();
    }

    private static void initOpenAL() {
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        context = ALC10.alcCreateContext(device, (int[]) null);
        ALC10.alcMakeContextCurrent(context);

        ALC.createCapabilities(device);
        org.lwjgl.openal.AL.createCapabilities(ALC.getCapabilities());

        source = AL10.alGenSources();
        AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);
    }

    private static void startAsyncDecode() {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Naruto-Audio-Loader");
            t.setDaemon(true);
            return t;
        });
        executor.submit(() -> {
            Process process = null;

            try {
                ProcessBuilder pb = new ProcessBuilder(
                        FFMPEG_PATH,
                        "-i", VIDEO_PATH,
                        "-vn",                      // 不要视频
                        "-f", "s16le",              // PCM 16bit little endian
                        "-ac", "2",                 // 双声道
                        "-ar", "44100",             // 44.1kHz
                        "-loglevel", "error",
                        "-"
                );

                process = pb.start();
                InputStream in = process.getInputStream();

                byte[] buffer = new byte[4096];
                int read;

                while (!cancelled && (read = in.read(buffer)) != -1) {
                    ByteBuffer data = MemoryUtil.memAlloc(read);
                    data.put(buffer, 0, read).flip();

                    int buf = AL10.alGenBuffers();
                    AL10.alBufferData(buf, AL10.AL_FORMAT_STEREO16, data, 44100);
                    MemoryUtil.memFree(data);

                    AL10.alSourceQueueBuffers(source, buf);

                    int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                    if (state != AL10.AL_PLAYING) {
                        AL10.alSourcePlay(source);
                    }

                    // 回收已经播放完的 buffer
                    int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
                    while (processed-- > 0) {
                        int old = AL10.alSourceUnqueueBuffers(source);
                        AL10.alDeleteBuffers(old);
                    }
                }

            } catch (Exception e) {
                System.out.println("Audio decode error: " + e.getMessage());
            } finally {
                if (process != null) process.destroyForcibly();
            }
        });
    }

    public static void shutdown() {
        cancelled = true;

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }

        if (source != 0) {
            AL10.alSourceStop(source);

            // 删除剩余 buffer
            int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
            while (queued-- > 0) {
                int buf = AL10.alSourceUnqueueBuffers(source);
                AL10.alDeleteBuffers(buf);
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
