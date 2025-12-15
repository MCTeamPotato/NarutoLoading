package me.kall.narutoloading.core.execution;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.longs.LongObjectImmutablePair;
import it.unimi.dsi.fastutil.longs.LongObjectPair;
import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.data.VideoArgs;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.NarutoLoading;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public final class NarutoVideoExecutor {
    public static final NarutoVideoExecutor INSTANCE = new NarutoVideoExecutor();

    public @Nullable LinkedBlockingQueue<LongObjectPair<NativeImage>> frameQueue;

    private @Nullable ExecutorService executor;
    private volatile boolean canceled;
    private @Nullable Process process;
    private long frameCounts;

    private @Nullable InputStream inputStream;
    private @Nullable ReadableByteChannel channel;

    public void setup(String sec) {
        this.canceled = false;
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoFrameExecutor");
            thread.setDaemon(true);
            return thread;
        });
        this.frameQueue = new LinkedBlockingQueue<>(NarutoConfig.BUFFER_SIZE);
        this.executor.submit(() -> {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    NarutoConfig.FFMPEG_PATH,
                    "-ss", sec,
                    "-i", NarutoConfig.video(),
                    "-vf", "format=rgb24,scale=" + VideoArgs.widthString() + ":" + VideoArgs.heightString(),
                    "-pix_fmt", "rgb24",
                    "-f", "image2pipe",
                    "-vcodec", "rawvideo",
                    "-loglevel", "error", "-"
            );

            try {
                int frameSize = VideoArgs.width() * VideoArgs.height() * 3;
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

                    NativeImage image = this.buildImage(buffer);
                    this.frameCounts++;
                    this.frameQueue.put(new LongObjectImmutablePair<>(this.frameCounts, image));
                }
            } catch (Exception exception) {
                if (NarutoConfig.DEBUG) NarutoLoading.LOGGER.error("Error occurs in NarutoVideoExecutor but hopefully this is ignorable.", exception);
            }
        });
        NarutoLoading.LOGGER.info("NarutoVideoExecutor sets up successfully");
    }

    public void setup() {
        setup("0");
    }

    public @Nullable NativeImage fetchImage(double elapsedSeconds) {
        if (this.frameQueue == null || this.frameQueue.isEmpty()) return null;
        LongObjectPair<NativeImage> frame = this.frameQueue.poll();
        if (frame == null) return null;

        boolean hasSkipping = false;

        while (frame != null && ((double) frame.firstLong()) / ((double) VideoArgs.fps()) < elapsedSeconds) {
            frame.right().close();
            frame = this.frameQueue.poll();
            hasSkipping = true;
        }

        if (hasSkipping && frame == null) NarutoRenderer.INSTANCE.lifetime.detectLagSpike();

        return frame == null ? null : frame.right();
    }

    public @NotNull NativeImage buildImage(byte @NotNull [] buffer) {
        NativeImage image = new NativeImage(VideoArgs.width(), VideoArgs.height(), false);
        for (int i = 0; i < buffer.length; i += 3) {
            int b = buffer[i] & 0xFF;
            int g = buffer[i + 1] & 0xFF;
            int r = buffer[i + 2] & 0xFF;
            int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
            image.setPixelRGBA(i / 3 % VideoArgs.width(), i / 3 / VideoArgs.width(), argb);
        }
        return image;
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
