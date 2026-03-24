package me.kall.narutoloading.common.executor.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public abstract class AbstractVideoExecutor<T> extends AbstractFFmpegExecutor {
    protected final Logger LOGGER = LogManager.getLogger(this.getClass());

    protected @Nullable LinkedBlockingQueue<Frame<T>> frameQueue;

    protected long frameIndex;

    protected @Nullable ReadableByteChannel channel;

    protected final Supplier<Runnable> lagSpikeHandler;

    protected final Supplier<String> video;
    protected final IntSupplier width, height;
    protected final DoubleSupplier fps;
    protected final IntSupplier bufferSize;

    protected AbstractVideoExecutor(Supplier<Runnable> lagSpikeHandler, Supplier<String> ffmpeg, Supplier<String> video, IntSupplier width, IntSupplier height, DoubleSupplier fps, IntSupplier bufferSize, BooleanSupplier debug) {
        super(ffmpeg, debug);
        this.lagSpikeHandler = lagSpikeHandler;
        this.video = video;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.bufferSize = bufferSize;
    }

    @Override
    public void setup(String sec) {
        this.frameIndex = (long) (Double.parseDouble(sec) * this.fps.getAsDouble());
        this.frameQueue = new LinkedBlockingQueue<>(this.bufferSize.getAsInt());

        super.setup(sec);
    }

    @Override
    protected ProcessBuilder buildProcess(String sec) {
        return new ProcessBuilder(
                this.ffmpeg.get(),
                "-ss", sec,
                "-i", this.video.get(),
                "-vf", "format=rgb24,scale=" + this.width.getAsInt() + ":" + this.height.getAsInt(),
                "-pix_fmt", "rgb24",
                "-f", "image2pipe",
                "-vcodec", "rawvideo",
                "-loglevel", "error", "-"
        );
    }

    @Override
    protected void runLoop(InputStream inputStream) throws Exception {
        int frameSize = this.width.getAsInt() * this.height.getAsInt() * 3;

        this.channel = Channels.newChannel(inputStream);
        assert this.channel != null;
        ByteBuffer buffer = ByteBuffer.allocateDirect(frameSize);

        assert this.frameQueue != null;
        while (!this.canceled) {
            buffer.clear();

            while (buffer.hasRemaining()) {
                if (this.channel.read(buffer) == -1) return;
            }

            buffer.flip();

            T frameData = buildFrame(buffer, frameSize);

            this.frameIndex++;
            this.frameQueue.put(new Frame<>(this.frameIndex, frameData));
        }
    }

    public @Nullable T fetch(double elapsedSeconds) {
        if (this.frameQueue == null || this.frameQueue.isEmpty()) return null;

        Frame<T> frame = this.frameQueue.poll();
        if (frame == null) return null;

        boolean skipped = false;

        while (frame != null && ((double) frame.index) / this.fps.getAsDouble() < elapsedSeconds) {
            release(frame.data);
            frame = this.frameQueue.poll();
            skipped = true;
        }

        if (skipped && frame == null) {
            this.lagSpikeHandler.get().run();
        }

        return frame == null ? null : frame.data;
    }

    @Override
    public void cleanup() {
        try {
            if (this.channel != null) {
                this.channel.close();
                this.channel = null;
            }
        } catch (Exception e) {
            LOGGER.error("Channel cleanup error", e);
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