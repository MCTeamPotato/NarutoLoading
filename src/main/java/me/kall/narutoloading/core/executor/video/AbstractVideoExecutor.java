package me.kall.narutoloading.core.executor.video;

import me.kall.narutoloading.data.Paths;
import me.kall.narutoloading.core.executor.AbstractFFmpegExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public abstract class AbstractVideoExecutor<T> extends AbstractFFmpegExecutor {
    protected @Nullable LinkedBlockingQueue<Frame<T>> frames;
    protected long frameIndex;
    protected @Nullable ReadableByteChannel channel;

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
        this.frameIndex = (long) (Double.parseDouble(seconds) * this.fps.getAsDouble());
        this.frames = new LinkedBlockingQueue<>(60);
        super.setup(seconds);
    }

    @Override
    protected String[] command(String seconds) {
        return new String[]{Paths.FFMPEG.toString(), "-ss", seconds, "-i", this.video.get(), "-vf", "format=rgb24,scale=" + this.width.getAsInt() + ":" + this.height.getAsInt(), "-pix_fmt", "rgb24", "-f", "image2pipe", "-vcodec", "rawvideo", "-loglevel", "error", "-"};
    }

    @Override
    protected void runLoop(@NotNull InputStream inputStream) throws Exception{
        int frameSize = this.width.getAsInt() * this.height.getAsInt() * 3;

        this.channel = Channels.newChannel(inputStream);
        ByteBuffer buffer = ByteBuffer.allocateDirect(frameSize);

        assert this.channel != null;
        assert this.frames != null;

        while (!this.canceled) {
            buffer.clear();

            while (buffer.hasRemaining()) {
                if (this.channel.read(buffer) == -1) return;
            }

            T frameData = this.buildFrame(buffer.flip(), frameSize);
            this.frameIndex++;
            this.frames.put(new Frame<>(this.frameIndex, frameData));
        }
    }

    public @Nullable T fetch(double elapsedSeconds) {
        if (this.frames == null || this.frames.isEmpty()) return null;

        Frame<T> frame = this.frames.poll();
        if (frame == null) return null;

        boolean skipped = false;

        while (frame != null && ((double) frame.index) / this.fps.getAsDouble() < elapsedSeconds) {
            this.release(frame.data);
            frame = this.frames.poll();
            skipped = true;
        }

        if (skipped && frame == null && this.onLagSpike != null) this.onLagSpike.get().run();

        return frame == null ? null : frame.data;
    }

    @Override
    public void cleanup() {
        if (this.channel != null) {
            try {
                this.channel.close();
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
            this.channel = null;
        }

        if (this.frames != null) {
            for (Frame<T> frame : this.frames) {
                this.release(frame.data);
            }
            this.frames = null;
        }
    }

    protected abstract T buildFrame(ByteBuffer buffer, int frameSize);

    protected abstract void release(T frame);

    protected record Frame<T>(long index, T data) {}
}
