package me.kall.narutoloading.common.executor;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class NarutoVideoExecutor {
    private @Nullable LinkedBlockingQueue<Frame> frameQueue;

    private @Nullable ExecutorService executor;
    private volatile boolean canceled;
    private @Nullable Process process;
    private long frameIndex;

    private @Nullable InputStream inputStream;
    private @Nullable ReadableByteChannel channel;

    private final Runnable detectLagSpike;

    private final Supplier<String> ffmpeg, widthString, heightString, video;
    private final IntSupplier width, height, fps;

    public NarutoVideoExecutor(Runnable detectLagSpike, Supplier<String> ffmpeg, Supplier<String> widthString, Supplier<String> heightString, Supplier<String> video, IntSupplier width, IntSupplier height, IntSupplier fps) {
        this.detectLagSpike = detectLagSpike;
        this.ffmpeg = ffmpeg;
        this.widthString = widthString;
        this.heightString = heightString;
        this.video = video;
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    public void setup(String sec) {
        this.canceled = false;
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoVideoExecutor");
            thread.setDaemon(true);
            return thread;
        });
        this.frameQueue = new LinkedBlockingQueue<>(BaseEnv.narutoConfig.bufferSize);
        this.executor.submit(() -> {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    this.ffmpeg.get(),
                    "-ss", sec,
                    "-i", this.video.get(),
                    "-vf", "format=rgb24,scale=" + this.widthString + ":" + this.heightString,
                    "-pix_fmt", "rgb24",
                    "-f", "image2pipe",
                    "-vcodec", "rawvideo",
                    "-loglevel", "error", "-"
            );

            try {
                int frameSize = this.width.getAsInt() * this.height.getAsInt() * 3;
                this.process = processBuilder.start();
                this.inputStream = this.process.getInputStream();
                this.channel = Channels.newChannel(this.inputStream);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(frameSize);

                while (!this.canceled) {
                    byteBuffer.clear();
                    while (byteBuffer.hasRemaining()) {
                        if (this.channel == null) return;
                        if (this.channel.read(byteBuffer) == -1) return;
                    }
                    byteBuffer.flip();
                    byte[] buffer = new byte[frameSize];
                    byteBuffer.get(buffer);

                    this.frameIndex++;
                    this.frameQueue.put(new Frame(this.frameIndex, buildImage(buffer)));
                }
            } catch (Exception exception) {
                if (BaseEnv.narutoConfig.debug) NarutoLoading.LOGGER.error("Error occurs in NarutoVideoExecutor but hopefully this is ignorable.", exception);
            }
        });
        NarutoLoading.LOGGER.info("NarutoVideoExecutor sets up successfully");
    }

    private @NotNull NativeImage buildImage(byte @NotNull [] buffer) {
        int width = this.width.getAsInt();
        int height = this.height.getAsInt();
        NativeImage image = new NativeImage(width, height, false);
        for (int i = 0; i < buffer.length; i += 3) {
            int b = buffer[i] & 0xFF;
            int g = buffer[i + 1] & 0xFF;
            int r = buffer[i + 2] & 0xFF;
            int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
            image.setPixelRGBA(i / 3 % width, i / 3 / width, argb);
        }
        return image;
    }

    public void setup() {
        setup("0");
    }

    public @Nullable NativeImage fetchImage(double elapsedSeconds) {
        if (this.frameQueue == null || this.frameQueue.isEmpty()) return null;
        Frame frame = this.frameQueue.poll();
        if (frame == null) return null;

        boolean hasSkipping = false;

        while (frame != null && ((double) frame.frameIndex()) / ((double) this.fps.getAsInt()) < elapsedSeconds) {
            frame.image.close();
            frame = this.frameQueue.poll();
            hasSkipping = true;
        }

        if (hasSkipping && frame == null) this.detectLagSpike.run();

        return frame == null ? null : frame.image();
    }

    public void shutdown(long frameElapsed) {
        if (this.canceled) return;
        this.canceled = true;

        try {
            if (this.inputStream != null) {
                this.inputStream.close();
                this.inputStream = null;
            }

            if (this.channel != null) {
                this.channel.close();
                this.channel = null;
            }
        } catch (Exception exception) {
            NarutoLoading.LOGGER.error("Error occurs in NarutoVideoExecutor resources cleanup during shutdown", exception);
        }

        if (this.process != null) {
            this.process.destroyForcibly();
            this.process = null;
        }

        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }

        if (this.frameQueue != null) {
            for (Frame frame : this.frameQueue) {
                frame.image.close();
            }
            this.frameQueue = null;
        }

        this.frameIndex = frameElapsed;
        NarutoLoading.LOGGER.info("NarutoVideoExecutor shuts down successfully");
    }

    public void shutdown() {
        this.shutdown(0L);
    }

    private record Frame(long frameIndex, NativeImage image) {}
}
