package me.kall.narutoloading;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class NarutoFrameExecutor {
    public static final BlockingQueue<NativeImage> frameQueue = new LinkedBlockingQueue<>(120);
    public static ExecutorService executor;
    public static boolean canceled;
    private static Process process;

    @SuppressWarnings("BusyWait")
    public static void setup() {
        canceled = false;
        executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoFrameExecutor");
            thread.setDaemon(true);
            return thread;
        });
        executor.submit(() -> {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    NarutoLoading.FFMPEG_PATH, "-i", NarutoLoading.VIDEO_PATH,
                    "-vf", "format=rgb24,scale=854:480",
                    "-pix_fmt", "rgb24",
                    "-f", "image2pipe",
                    "-vcodec", "rawvideo",
                    "-loglevel", "error",
                    "-"
            );

            try {
                process = processBuilder.start();
                InputStream inputStream = process.getInputStream();

                byte[] buffer = new byte[854 * 480 * 3];
                int frameSize = buffer.length;

                while (!canceled) {
                    int read = 0;
                    while (read < frameSize) {
                        int r = inputStream.read(buffer, read, frameSize - read);
                        if (r == -1) return;
                        read += r;
                    }

                    NativeImage nativeImage = getNativeImage(buffer);

                    frameQueue.put(nativeImage);

                    while (frameQueue.size() > 110) {
                        Thread.sleep(1);
                    }
                }
            } catch (Exception ignored) {}
        });
        NarutoAudioExecutor.setup();
    }

    public static @NotNull NativeImage getNativeImage(byte @NotNull [] buffer) {
        NativeImage image = new NativeImage(854, 480, false);
        for (int i = 0; i < buffer.length; i += 3) {
            int b = buffer[i] & 0xFF;
            int g = buffer[i + 1] & 0xFF;
            int r = buffer[i + 2] & 0xFF;
            int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
            image.setPixelRGBA(i/3 % 854, i/3 / 854, argb);
        }
        return image;
    }

    public static void shutdown() {
        canceled = true;
        if (executor != null && !executor.isShutdown()) {
            if (process != null) {
                process.destroyForcibly();
                process = null;
            }
            executor.shutdownNow();
            executor = null;
        }
        frameQueue.clear();
        NarutoAudioExecutor.shutdown();
    }
}
