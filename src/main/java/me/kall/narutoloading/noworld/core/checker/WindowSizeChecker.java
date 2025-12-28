package me.kall.narutoloading.noworld.core.checker;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import me.kall.narutoloading.common.env.BaseEnv;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;

public final class WindowSizeChecker {
    private int lastWidth = -1;
    private int lastHeight = -1;

    private boolean resizable = false;
    private final NarutoRenderer renderer;

    public WindowSizeChecker(NarutoRenderer renderer) {
        this.renderer = renderer;
    }

    public void clientTick(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        if (!this.renderer.isRunning() || !this.renderer.isEnabled()) {
            reset();
            return;
        }

        int width = BaseEnv.narutoConfig.width();
        int height = BaseEnv.narutoConfig.height();

        if (this.lastWidth == -1 && this.lastHeight == -1) {
            this.lastWidth = width;
            this.lastHeight = height;
            return;
        }

        if (width != this.lastWidth || height != this.lastHeight) {
            NarutoLoading.LOGGER.info("Window size changed from [{}, {}] to [{}, {}]", lastWidth, lastHeight, width, height);

            this.lastWidth = width;
            this.lastHeight = height;
            this.resizable = true;
        }
    }

    private void reset() {
        this.lastWidth = -1;
        this.lastHeight = -1;
        this.resizable = false;
    }

    public void resize() {
        if (this.resizable){
            this.resizable = false;
            String currentSecond = String.valueOf(renderer.lifetime.elapsedSeconds());
            NarutoLoading.LOGGER.info("Resizing Naruto Loading video from {} seconds", currentSecond);

            this.renderer.videoExecutor.shutdown((long) (this.renderer.lifetime.elapsedSeconds() * BaseEnv.noWorldVideoArgs.fps()));
            this.renderer.videoExecutor.setup(currentSecond);

            if (this.renderer.dynamicTexture != null) this.renderer.dynamicTexture.close();

            this.renderer.dynamicTexture = new DynamicTexture(BaseEnv.narutoConfig.width(), BaseEnv.narutoConfig.height(), false);
            this.renderer.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", this.renderer.dynamicTexture);
        }
    }
}