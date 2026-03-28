package me.kall.narutoloading.fade;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public class FadeCenter {
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;

    private static int stopTickCount = 0;
    private static float fadeAlpha = 1.0F;

    private static boolean shouldFade() {
        return stopTickCount >= 20 * 5;
    }

    public static boolean transparency() {
        return fadeAlpha == 0.0F;
    }

    public static float fadeAlpha() {
        return fadeAlpha;
    }

    public static int modifyAlpha(int color) {
        if (!shouldFade() || fadeAlpha == 1.0F) return color;
        if (transparency()) return (color & 0x00FFFFFF);
        int alpha = (int)(fadeAlpha * 255.0F) & 0xFF;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (disabled()) return;
            Minecraft minecraft = Minecraft.getInstance();
            MouseHandler mouseHandler = minecraft.mouseHandler;

            double x = mouseHandler.xpos();
            double y = mouseHandler.ypos();

            if (init(x, y)) return;

            stopTickCount = lastMouseX == x && lastMouseY == y ? stopTickCount + 1 : 0;

            lastMouseX = x;
            lastMouseY = y;

            fadeAlpha = shouldFade() ? Math.max(0.0F, fadeAlpha - 0.05F) : Math.min(1.0F, fadeAlpha + 0.05F);
        }
    }

    @SubscribeEvent
    public static void type(InputEvent event) {
        Minecraft.getInstance().execute(FadeCenter::reset);
    }

    private static void reset() {
        fadeAlpha = 1.0F;
        lastMouseX = Double.NaN;
        lastMouseY = Double.NaN;
        stopTickCount = 0;
    }

    private static boolean disabled() {
        if (!NarutoRenderer.getInstance().isRunning() || !NarutoRenderer.getInstance().isRunnable()) {
            reset();
            return true;
        }

        return false;
    }

    private static boolean init(double x, double y) {
        if (Double.isNaN(lastMouseX) || Double.isNaN(lastMouseY)) {
            lastMouseX = x;
            lastMouseY = y;
            return true;
        }

        return false;
    }
}
