package me.kall.narutoloading.core.detection;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.data.VideoArgs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;

public final class WindowSizeChecker {
    private int lastWidth = -1;
    private int lastHeight = -1;

    private boolean resizable = false;

    public void clientTick(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft minecraft = Minecraft.getInstance();

        if (!NarutoRenderer.INSTANCE.isRunning() || minecraft.level != null) {
            reset();
            return;
        }

        int width = VideoArgs.width();
        int height = VideoArgs.height();

        if (lastWidth == -1 && lastHeight == -1) {
            lastWidth = width;
            lastHeight = height;
            return;
        }

        if (width != lastWidth || height != lastHeight) {
            NarutoLoading.LOGGER.info("Window size changed from [{}, {}] to [{}, {}]", lastWidth, lastHeight, width, height);

            lastWidth = width;
            lastHeight = height;
            resizable = true;
        }
    }

    private void reset() {
        lastWidth = -1;
        lastHeight = -1;
        resizable = false;
    }

    public void resize(NarutoRenderer renderer) {
        if (resizable){
            resizable = false;
            String currentSecond = String.valueOf(renderer.lifetime.elapsedSeconds());
            NarutoLoading.LOGGER.info("Resizing Naruto Loading video from {} seconds", currentSecond);

            renderer.narutoVideoExecutor.shutdown(renderer.lifetime.frameCount());
            renderer.narutoVideoExecutor.setup(currentSecond);

            if (renderer.dynamicTexture != null) renderer.dynamicTexture.close();

            renderer.dynamicTexture = new DynamicTexture(VideoArgs.width(), VideoArgs.height(), false);
            renderer.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", renderer.dynamicTexture);
        }
    }
}