package me.kall.narutoloading.client.renderer;

import org.jetbrains.annotations.Nullable;

public interface FrameRenderer<T> {
    void setup(int width, int height, double seekTo, boolean ready);

    void update(int width, int height, @Nullable T frame);

    void render();

    void shutdown();
}
