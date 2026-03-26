package me.kall.narutoloading.core.executor.video;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class NarutoVideoExecutor extends AbstractVideoExecutor<NativeImage> {
    public NarutoVideoExecutor(@Nullable Supplier<Runnable> onLagSpike, Supplier<String> video, IntSupplier width, IntSupplier height, DoubleSupplier fps) {
        super(onLagSpike, video, width, height, fps);
    }

    @Override
    protected NativeImage buildFrame(@NotNull ByteBuffer buffer, int frameSize) {
        byte[] arr = new byte[frameSize];
        buffer.get(arr);

        int w = this.width.getAsInt();
        int h = this.height.getAsInt();

        NativeImage img = new NativeImage(w, h, false);

        for (int i = 0; i < arr.length; i += 3) {
            int b = arr[i] & 0xFF;
            int g = arr[i + 1] & 0xFF;
            int r = arr[i + 2] & 0xFF;
            int argb = 0xFF000000 | (r << 16) | (g << 8) | b;

            img.setPixelRGBA(i / 3 % w, i / 3 / w, argb);
        }

        return img;
    }

    @Override
    protected void release(@NotNull NativeImage frame) {
        frame.close();
    }
}
