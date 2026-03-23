package me.kall.narutoloading.common.executor.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public abstract class AbstractVideoExecutor<T> {
    protected final Logger LOGGER = LogManager.getLogger(this.getClass());

    protected @Nullable LinkedBlockingQueue<Frame<T>> frameQueue;

    protected @Nullable ExecutorService executor;
    protected volatile boolean canceled;
    protected @Nullable Process process;
    protected long frameIndex;

    protected @Nullable InputStream inputStream;
    protected @Nullable ReadableByteChannel channel;

    protected final Supplier<Runnable> lagSpikeHandler;

    protected final Supplier<String> ffmpeg, video;
    protected final IntSupplier width, height;
    protected final DoubleSupplier fps;
    protected final IntSupplier bufferSize;
    protected final BooleanSupplier debug;

    protected AbstractVideoExecutor(Supplier<Runnable> lagSpikeHandler, Supplier<String> ffmpeg, Supplier<String> video, IntSupplier width, IntSupplier height, DoubleSupplier fps, IntSupplier bufferSize, BooleanSupplier debug) {
        this.lagSpikeHandler = lagSpikeHandler;
        this.ffmpeg = ffmpeg;
        this.video = video;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.bufferSize = bufferSize;
        this.debug = debug;
    }

    public void setup(String sec) {
        this.canceled = false;

        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, this.getClass().getSimpleName());
            thread.setDaemon(true);
            return thread;
        });

        this.frameIndex = (long) (Double.parseDouble(sec) * this.fps.getAsDouble());
        this.frameQueue = new LinkedBlockingQueue<>(this.bufferSize.getAsInt());

        this.executor.submit(() -> {
            ProcessBuilder pb = new ProcessBuilder(
                    this.ffmpeg.get(),
                    "-ss", sec,
                    "-i", this.video.get(),
                    "-vf", "format=rgb24,scale=" + this.width.getAsInt() + ":" + this.height.getAsInt(),
                    "-pix_fmt", "rgb24",
                    "-f", "image2pipe",
                    "-vcodec", "rawvideo",
                    "-loglevel", "error", "-"
            );

            try {
                int frameSize = this.width.getAsInt() * this.height.getAsInt() * 3;

                this.process = pb.start();
                this.inputStream = this.process.getInputStream();
                this.channel = Channels.newChannel(this.inputStream);

                ByteBuffer buffer = ByteBuffer.allocateDirect(frameSize);

                while (!this.canceled) {
                    buffer.clear();

                    while (buffer.hasRemaining()) {
                        if (this.channel == null) return;
                        if (this.channel.read(buffer) == -1) return;
                    }

                    buffer.flip();

                    T frameData = buildFrame(buffer, frameSize);

                    this.frameIndex++;
                    this.frameQueue.put(new Frame<>(this.frameIndex, frameData));
                }
            } catch (Exception e) {
                if (this.debug.getAsBoolean()) {
                    LOGGER.error("Video executor error", e);
                }
            }
        });
    }

    public void setup() {
        setup("0");
    }

    public @Nullable T fetch(double elapsedSeconds) {
        if (this.frameQueue == null || this.frameQueue.isEmpty()) return null;

        Frame<T> frame = this.frameQueue.poll();
        if (frame == null) return null;

        boolean hasSkipping = false;

        while (frame != null && ((double) frame.index) / this.fps.getAsDouble() < elapsedSeconds) {
            release(frame.data);
            frame = this.frameQueue.poll();
            hasSkipping = true;
        }

        if (hasSkipping && frame == null) {
            this.lagSpikeHandler.get().run();
        }

        return frame == null ? null : frame.data;
    }

    public void shutdown() {
        this.canceled = true;

        if (this.process != null) {
            this.process.destroyForcibly();
            this.process = null;
        }

        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }

        try {
            if (this.inputStream != null) {
                this.inputStream.close();
                this.inputStream = null;
            }

            if (this.channel != null) {
                this.channel.close();
                this.channel = null;
            }
        } catch (Exception e) {
            LOGGER.error("Cleanup error", e);
        }

        if (this.frameQueue != null) {
            for (Frame<T> frame : this.frameQueue) {
                release(frame.data);
            }
            this.frameQueue = null;
        }
    }

    protected abstract T buildFrame(ByteBuffer buffer, int frameSize);

    protected abstract void release(T frame);

    protected record Frame<T>(long index, T data) {}
}