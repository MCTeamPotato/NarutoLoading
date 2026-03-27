package me.kall.narutoloading.core.executor.video;

import me.kall.narutoloading.core.executor.AbstractFFmpegExecutor;
import me.kall.narutoloading.data.Paths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public abstract class AbstractVideoExecutor<T> extends AbstractFFmpegExecutor {
    private final AtomicReference<LinkedBlockingQueue<Frame<T>>> frames = new AtomicReference<>();
    private final AtomicLong frameIndex = new AtomicLong(0);
    private final AtomicReference<ReadableByteChannel> channel = new AtomicReference<>();

    protected final @Nullable Supplier<Runnable> onLagSpike;
    protected final Supplier<String> video;
    protected final IntSupplier width, height;
    protected final DoubleSupplier fps;

    protected AbstractVideoExecutor(@Nullable Supplier<Runnable> onLagSpike, Supplier<String> video, IntSupplier width, IntSupplier height, DoubleSupplier fps) {
        this.onLagSpike = onLagSpike;
        this.video = video;
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    @Override
    public void setup(String seconds) {
        this.frameIndex.set((long)(Double.parseDouble(seconds) * this.fps.getAsDouble()));
        this.frames.set(new LinkedBlockingQueue<>(60));
        super.setup(seconds);
    }

    @Override
    protected String[] command(String seconds) {
        return new String[]{Paths.FFMPEG.toString(), "-ss", seconds, "-i", this.video.get(), "-vf", "format=rgb24,scale=" + this.width.getAsInt() + ":" + this.height.getAsInt(), "-pix_fmt", "rgb24", "-f", "image2pipe", "-vcodec", "rawvideo", "-loglevel", "error", "-"};
    }

    @Override
    protected void runLoop(@NotNull InputStream inputStream) throws Exception {
        int frameSize = this.width.getAsInt() * this.height.getAsInt() * 3;

        ReadableByteChannel channel = Channels.newChannel(inputStream);
        this.channel.set(channel);

        LinkedBlockingQueue<Frame<T>> frames = this.frames.get();
        if (frames == null) return;

        ByteBuffer buffer = ByteBuffer.allocateDirect(frameSize);

        while (!this.canceled.get()) {
            buffer.clear();
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) == -1) return;
            }
            T frameData = this.buildFrame(buffer.flip(), frameSize);
            frames.put(new Frame<>(this.frameIndex.incrementAndGet(), frameData));
        }
    }

    public @Nullable T fetch(double elapsedSeconds) {
        LinkedBlockingQueue<Frame<T>> queue = this.frames.get();
        if (queue == null || queue.isEmpty()) return null;

        Frame<T> frame = queue.poll();
        if (frame == null) return null;

        boolean skipped = false;
        while (frame != null && (double) frame.index() / this.fps.getAsDouble() < elapsedSeconds) {
            this.release(frame.data());
            frame = queue.poll();
            skipped = true;
        }

        if (skipped && frame == null && this.onLagSpike != null) this.onLagSpike.get().run();
        return frame == null ? null : frame.data();
    }

    @Override
    public void cleanup() {
        ReadableByteChannel channel = this.channel.getAndSet(null);
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        LinkedBlockingQueue<Frame<T>> frames = this.frames.getAndSet(null);
        if (frames != null) {
            for (Frame<T> f : frames) {
                this.release(f.data());
            }
        }
    }

    protected abstract T buildFrame(ByteBuffer buffer, int frameSize);
    protected abstract void release(T frame);

    protected record Frame<T>(long index, T data) {}
}