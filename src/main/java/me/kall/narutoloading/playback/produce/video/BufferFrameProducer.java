package me.kall.narutoloading.playback.produce.video;

import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.config.NarutoConfig;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class BufferFrameProducer extends AbstractFrameProducer<ByteBuffer> {
    private static final double LOADING_BUFFER_SECONDS = 1D;

    private final Queue<ByteBuffer> free = new ConcurrentLinkedQueue<>();
    private final Set<ByteBuffer> all = ConcurrentHashMap.newKeySet();

    public BufferFrameProducer(String ffmpeg, VideoArgs videoArgs, boolean gpuAccel) {
        this(ffmpeg, videoArgs, gpuAccel, NarutoConfig.limitWidth(), NarutoConfig.limitHeight(), DEFAULT_MAX_BUFFER_BYTES, loadingFrames(videoArgs));
    }

    public BufferFrameProducer(String ffmpeg, VideoArgs videoArgs, boolean gpuAccel, int maxWidth, int maxHeight, long maxBufferBytes, int targetFrames) {
        super(ffmpeg, videoArgs, gpuAccel, frameSize(videoArgs, maxWidth, maxHeight), maxWidth, maxHeight, maxBufferBytes, targetFrames);
    }

    private static int loadingFrames(@NotNull VideoArgs args) {
        return Math.max(1, (int) (args.fps() * LOADING_BUFFER_SECONDS));
    }

    private static int frameSize(@NotNull VideoArgs args, int maxWidth, int maxHeight) {
        int width = args.scaledWidth(maxWidth, maxHeight);
        int height = args.scaledHeight(maxWidth, maxHeight);
        return width * height + 2 * ((width / 2) * (height / 2));
    }

    @Override
    protected void onSetup(double seekTo) {
        super.onSetup(seekTo);

        int preallocate = Math.min(this.bufferCapacity, this.warmFrames);

        for (int index = 0; index < preallocate; index++) {
            ByteBuffer buffer = MemoryUtil.memAlloc(this.frameSize);
            this.all.add(buffer);
            this.free.offer(buffer);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();

        for (ByteBuffer buffer : this.all) MemoryUtil.memFree(buffer);

        this.all.clear();
        this.free.clear();
    }

    @Override
    protected @NotNull String frameType() {
        return "yuv420p";
    }

    @Override
    protected @NotNull ByteBuffer next() {
        ByteBuffer buffer = this.free.poll();

        if (buffer == null) {
            buffer = MemoryUtil.memAlloc(this.frameSize);
            this.all.add(buffer);
        }

        return buffer;
    }

    @Override
    protected void onBuilt(ByteBuffer frame, long frameIndex) throws InterruptedException {
        this.frames.put(new Frame<>(frame, frameIndex));
    }

    @Override
    protected void onDead(ByteBuffer frame) {
        this.free.offer(frame);
    }
}
