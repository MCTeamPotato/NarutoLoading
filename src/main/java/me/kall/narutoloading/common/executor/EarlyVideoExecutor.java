package me.kall.narutoloading.common.executor;

import me.kall.narutoloading.common.executor.base.AbstractVideoExecutor;

import java.nio.ByteBuffer;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class EarlyVideoExecutor extends AbstractVideoExecutor<ByteBuffer> {
    public EarlyVideoExecutor(
            Supplier<Runnable> lagSpikeHandler,
            Supplier<String> ffmpeg,
            Supplier<String> video,
            IntSupplier width,
            IntSupplier height,
            DoubleSupplier fps,
            IntSupplier bufferSize,
            BooleanSupplier debug
    ) {
        super(lagSpikeHandler, ffmpeg, video, width, height, fps, bufferSize, debug);
    }

    @Override
    protected ByteBuffer buildFrame(ByteBuffer buffer, int frameSize) {
        return ByteBuffer.allocateDirect(frameSize).put(buffer).flip();
    }

    @Override protected void release(ByteBuffer frame) {}

    public ByteBuffer fetchFrame(double elapsedSeconds) {
        return fetch(elapsedSeconds);
    }
}