package me.kall.narutoloading.core.executor.video;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class EarlyVideoExecutor extends AbstractVideoExecutor<ByteBuffer> {
    public EarlyVideoExecutor(@Nullable Supplier<Runnable> onLagSpike, Supplier<String> video, IntSupplier width, IntSupplier height, DoubleSupplier fps) {
        super(onLagSpike, video, width, height, fps);
    }

    @Override
    protected ByteBuffer buildFrame(ByteBuffer buffer, int frameSize) {
        return ByteBuffer.allocateDirect(frameSize).put(buffer).flip();
    }

    @Override protected void release(ByteBuffer frame) {}
}
