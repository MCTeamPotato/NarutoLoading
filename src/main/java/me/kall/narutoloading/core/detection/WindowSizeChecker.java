package me.kall.narutoloading.core.detection;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.data.VideoArgs;
import me.kall.narutoloading.core.execution.NarutoVideoExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public final class WindowSizeChecker {
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    private static boolean resizable = false;

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
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

    private static void reset() {
        lastWidth = -1;
        lastHeight = -1;
        resizable = false;
    }

    public static void resize(NarutoRenderer renderer) {
        if (resizable){
            resizable = false;
            String currentSecond = String.valueOf(renderer.lifetime.elapsedSeconds());
            NarutoLoading.LOGGER.info("Resizing Naruto Loading video from {} seconds", currentSecond);

            NarutoVideoExecutor.INSTANCE.shutdown(renderer.lifetime.frameCount());
            NarutoVideoExecutor.INSTANCE.setup(currentSecond);

            if (renderer.dynamicTexture != null) renderer.dynamicTexture.close();

            renderer.dynamicTexture = new DynamicTexture(VideoArgs.width(), VideoArgs.height(), false);
            renderer.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", renderer.dynamicTexture);
        }
    }
}