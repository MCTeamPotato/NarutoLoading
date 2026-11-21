package me.kall.narutoloading.produce.video;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.app.ffmpeg.data.VideoArgs;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class ImageFrameProducer extends AbstractFrameProducer<NativeImage> {
    private @Nullable ByteBuffer frame;

    public ImageFrameProducer(String ffmpeg, VideoArgs videoArgs, boolean gpuAccel, double bufferSeconds) {
        super(ffmpeg, videoArgs, gpuAccel, videoArgs.width() * videoArgs.height() * 4, bufferSeconds);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (this.frame != null) {
            MemoryUtil.memFree(this.frame);
            this.frame = null;
        }
    }

    @Override
    @Contract(pure = true)
    protected @NotNull String frameType() {
        return "rgba";
    }

    @Override
    protected ByteBuffer next() {
        if (this.frame == null) {
            this.frame = MemoryUtil.memAlloc(this.frameSize);
        } else {
            this.frame.clear();
        }
        return this.frame;
    }

    @Override
    protected void onBuilt(ByteBuffer frame, long frameIndex) throws InterruptedException {
        NativeImage image = new NativeImage(this.args.width(), this.args.height(), false);
        MemoryUtil.memCopy(MemoryUtil.memAddress(frame), image.pixels, (long) this.args.width() * (long) this.args.height() * 4L);
        this.frames.put(new Frame<>(image, frameIndex));
    }

    @Override
    protected void onDead(@NotNull NativeImage frame) {
        frame.close();
    }
}
