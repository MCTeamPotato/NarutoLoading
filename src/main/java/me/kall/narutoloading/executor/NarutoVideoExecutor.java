package me.kall.narutoloading.executor;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.longs.LongObjectImmutablePair;
import it.unimi.dsi.fastutil.longs.LongObjectPair;
import me.kall.narutoloading.NarutoLoading;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public final class NarutoVideoExecutor {
    public @Nullable LinkedBlockingQueue<LongObjectPair<NativeImage>> frameQueue;

    private @Nullable ExecutorService executor;
    private boolean canceled;
    private @Nullable Process process;
    private long frameCounts;

    public void setup(String sec) {
        this.canceled = false;
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoFrameExecutor");
            thread.setDaemon(true);
            return thread;
        });
        this.frameQueue = new LinkedBlockingQueue<>(60);
        this.executor.submit(() -> {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    NarutoLoading.FFMPEG_PATH,
                    "-ss", sec,
                    "-i", NarutoLoading.VIDEO_PATH,
                    "-vf", "format=rgb24,scale=" + NarutoLoading.widthString() + ":" + NarutoLoading.heightString(),
                    "-pix_fmt", "rgb24",
                    "-f", "image2pipe",
                    "-vcodec", "rawvideo",
                    "-loglevel", "error",
                    "-"
            );

            try {
                this.process = processBuilder.start();
                InputStream inputStream = this.process.getInputStream();

                byte[] buffer = new byte[NarutoLoading.width() * NarutoLoading.height() * 3];
                int frameSize = buffer.length;

                while (!this.canceled) {
                    int read = 0;
                    while (read < frameSize) {
                        int r = inputStream.read(buffer, read, frameSize - read);
                        if (r == -1) return;
                        read += r;
                    }

                    NativeImage image = this.buildImage(buffer);
                    this.frameCounts++;
                    this.frameQueue.put(new LongObjectImmutablePair<>(this.frameCounts, image));
                }
            } catch (Exception exception) {
                NarutoLoading.LOGGER.error("Error occurs in NarutoVideoExecutor", exception);
            }
        });
        NarutoLoading.LOGGER.info("NarutoVideoExecutor sets up successfully");
    }

    public void setup() {
        setup("0");
    }

    public @Nullable NativeImage fetchImage(long elapsedSeconds) {
        if (this.frameQueue == null || this.frameQueue.isEmpty()) return null;
        LongObjectPair<NativeImage> frame = this.frameQueue.poll();
        if (frame == null) return null;

        while (frame != null && frame.firstLong() / NarutoLoading.fps() < elapsedSeconds) {
            frame = this.frameQueue.poll();
        }

        return frame == null ? null : frame.right();
    }

    public @NotNull NativeImage buildImage(byte @NotNull [] buffer) {
        NativeImage image = new NativeImage(NarutoLoading.width(), NarutoLoading.height(), false);
        for (int i = 0; i < buffer.length; i += 3) {
            int b = buffer[i] & 0xFF;
            int g = buffer[i + 1] & 0xFF;
            int r = buffer[i + 2] & 0xFF;
            int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
            image.setPixelRGBA(i / 3 % NarutoLoading.width(), i / 3 / NarutoLoading.width(), argb);
        }
        return image;
    }

    public void shutdown(long frameElapsed) {
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

        if (this.frameQueue != null) {
            for (LongObjectPair<NativeImage> frame : this.frameQueue) {
                frame.right().close();
            }
            this.frameQueue = null;
        }

        this.frameCounts = frameElapsed;
        NarutoLoading.LOGGER.info("NarutoVideoExecutor shuts down successfully");
    }

    public void shutdown() {
        this.shutdown(0L);
    }
}
