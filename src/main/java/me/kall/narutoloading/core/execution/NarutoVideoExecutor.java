package me.kall.narutoloading.core.execution;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.data.FFmpeg;
import me.kall.narutoloading.data.NarutoConfig;
import me.kall.narutoloading.data.VideoArgs;
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

    private @Nullable LinkedBlockingQueue<Frame> frameQueue;

    private @Nullable ExecutorService executor;
    private volatile boolean canceled;
    private @Nullable Process process;
    private long frameIndex;

    private @Nullable InputStream inputStream;
    private @Nullable ReadableByteChannel channel;

    public void setup(String sec) {
        this.canceled = false;
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoFrameExecutor");
            thread.setDaemon(true);
            return thread;
        });
        this.frameQueue = new LinkedBlockingQueue<>(NarutoConfig.bufferSize);
        this.executor.submit(() -> {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    FFmpeg.ffmpeg,
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

                    this.frameIndex++;
                    this.frameQueue.put(new Frame(this.frameIndex, buffer));
                }
            } catch (Exception exception) {
                if (NarutoConfig.debug) NarutoLoading.LOGGER.error("Error occurs in NarutoVideoExecutor but hopefully this is ignorable.", exception);
            }
        });
        NarutoLoading.LOGGER.info("NarutoVideoExecutor sets up successfully");
    }

    public void setup() {
        setup("0");
    }

    public byte @Nullable [] fetchImage(double elapsedSeconds) {
        if (this.frameQueue == null || this.frameQueue.isEmpty()) return null;
        Frame frame = this.frameQueue.poll();
        if (frame == null) return null;

        boolean hasSkipping = false;

        while (frame != null && ((double) frame.frameIndex()) / ((double) VideoArgs.fps()) < elapsedSeconds) {
            frame = this.frameQueue.poll();
            hasSkipping = true;
        }

        if (hasSkipping && frame == null) NarutoRenderer.INSTANCE.lifetime.detectLagSpike();

        return frame == null ? null : frame.buffer();
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
            this.frameQueue.clear();
            this.frameQueue = null;
        }

        this.frameIndex = frameElapsed;
        NarutoLoading.LOGGER.info("NarutoVideoExecutor shuts down successfully");
    }

    public void shutdown() {
        this.shutdown(0L);
    }

    private record Frame(long frameIndex, byte[] buffer) {}
}
