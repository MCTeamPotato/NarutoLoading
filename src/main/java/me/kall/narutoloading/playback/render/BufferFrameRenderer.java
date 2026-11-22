package me.kall.narutoloading.playback.render;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public final class BufferFrameRenderer implements FrameRenderer<ByteBuffer> {
    private final boolean flipY;
    private final Loading loading = new Loading();

    private @Nullable YuvPresenter presenter;

    public BufferFrameRenderer(boolean flipY) {
        this.flipY = flipY;
    }

    @Override
    public void setup(int width, int height, double seekTo, boolean ready) {
        this.shutdown();

        YuvPresenter presenter = new YuvPresenter(this.flipY);
        presenter.setup(width, height);
        this.presenter = presenter;

        if (!ready) presenter.upload(this.loading.get(width, height));
    }

    @Override
    public void update(int width, int height, @Nullable ByteBuffer frame) {
        if (frame == null || this.presenter == null) return;
        this.presenter.upload(frame);
    }

    @Override
    public void render() {
        this.render(this.flipY);
    }

    public void render(boolean flipY) {
        if (this.presenter != null) this.presenter.present(flipY);
    }

    @Override
    public void shutdown() {
        if (this.presenter != null) {
            this.presenter.close();
            this.presenter = null;
        }
    }
}
