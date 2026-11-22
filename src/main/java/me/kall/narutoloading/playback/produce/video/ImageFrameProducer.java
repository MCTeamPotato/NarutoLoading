package me.kall.narutoloading.playback.produce.video;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.config.NarutoConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class ImageFrameProducer extends AbstractFrameProducer<NativeImage> {
    private @Nullable ByteBuffer frame;

    public ImageFrameProducer(String ffmpeg, VideoArgs videoArgs, boolean gpuAccel, double bufferSeconds) {
        super(ffmpeg, videoArgs, gpuAccel, videoArgs.scaledWidth(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()) * videoArgs.scaledHeight(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()) * 4, bufferSeconds);
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
        int width = this.args.scaledWidth(NarutoConfig.limitWidth(), NarutoConfig.limitHeight());
        int height = this.args.scaledHeight(NarutoConfig.limitWidth(), NarutoConfig.limitHeight());

        NativeImage image = new NativeImage(width, height, false);
        MemoryUtil.memCopy(MemoryUtil.memAddress(frame), image.pixels, (long) width * (long) height * 4L);
        this.frames.put(new Frame<>(image, frameIndex));
    }

    @Override
    protected void onDead(@NotNull NativeImage frame) {
        frame.close();
    }
}
