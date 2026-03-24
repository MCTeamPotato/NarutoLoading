package me.kall.narutoloading.noworld.core.checker;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoLoading.MOD_ID)
public final class WindowSizeChecker {
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    private static boolean resizable = false;

    @SubscribeEvent
    public static void clientTick(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        if (!NarutoRenderer.INSTANCE.isRunning() || !NarutoRenderer.INSTANCE.isEnabled()) {
            reset();
            return;
        }

        int width = BaseEnv.getNarutoConfig().width();
        int height = BaseEnv.getNarutoConfig().height();

        if (lastWidth == -1 && lastHeight == -1) {
            lastWidth = width;
            lastHeight = height;
            return;
        }

        if (width != lastWidth || height != lastHeight) {
            lastWidth = width;
            lastHeight = height;
            resizable = true;
        }

        resize();
    }

    private static void reset() {
        lastWidth = -1;
        lastHeight = -1;
        resizable = false;
    }

    public static void resize() {
        if (resizable){
            resizable = false;
            if (NarutoRenderer.INSTANCE.lifetime != null){
                String currentSecond = String.valueOf(NarutoRenderer.INSTANCE.lifetime.elapsedSeconds());
                if (NarutoRenderer.INSTANCE.videoExecutor != null) {
                    NarutoRenderer.INSTANCE.videoExecutor.shutdown();
                    NarutoRenderer.INSTANCE.videoExecutor.setup(currentSecond);
                }

                if (NarutoRenderer.INSTANCE.audioExecutor != null) {
                    NarutoRenderer.INSTANCE.audioExecutor.shutdown();
                    NarutoRenderer.INSTANCE.audioExecutor.setup(currentSecond);
                }
            }

            if (NarutoRenderer.INSTANCE.dynamicTexture != null) NarutoRenderer.INSTANCE.dynamicTexture.close();

            NarutoRenderer.INSTANCE.dynamicTexture = new DynamicTexture(BaseEnv.getNarutoConfig().width(), BaseEnv.getNarutoConfig().height(), false);
            NarutoRenderer.INSTANCE.textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", NarutoRenderer.INSTANCE.dynamicTexture);
        }
    }
}