package me.kall.narutoloading.noworld.core.checker;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public final class FadeChecker {
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;

    private static int stopTickCount = 0;
    private static float fadeAlpha = 1.0F;

    public static final Component EMPTY_COMPONENT = Component.empty();

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
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();

         if (FadeChecker.disabled()) return;

        MouseHandler mouse = minecraft.mouseHandler;

        double x = mouse.xpos();
        double y = mouse.ypos();
        if (FadeChecker.init(x, y)) return;

        stopTickCount = lastMouseX == x && lastMouseY == y ? stopTickCount + 1 : 0;

        lastMouseX = x;
        lastMouseY = y;

        fadeAlpha = FadeChecker.shouldFade() ? Math.max(0.0F, fadeAlpha - 0.05F) : Math.min(1.0F, fadeAlpha + 0.05F);
    }

    @SubscribeEvent public static void type(InputEvent.MouseButton.Pre event) {reset();}
    @SubscribeEvent public static void type(InputEvent.MouseButton.Post event) {reset();}
    @SubscribeEvent public static void type(InputEvent.MouseButton.MouseScrollingEvent event) {reset();}
    @SubscribeEvent public static void type(InputEvent.MouseButton.Key event) {reset();}
    @SubscribeEvent public static void type(InputEvent.MouseButton.InteractionKeyMappingTriggered event) {reset();}

    private static void reset() {
        if (Minecraft.getInstance().screen != null) {
            fadeAlpha = 1.0F;
            lastMouseX = Double.NaN;
            lastMouseY = Double.NaN;
            stopTickCount = 0;
        }
    }

    private static boolean disabled() {
        if (!NarutoRenderer.INSTANCE.isRunning() || !NarutoRenderer.INSTANCE.isEnabled()) {
            fadeAlpha = 1.0F;
            lastMouseX = Double.NaN;
            lastMouseY = Double.NaN;
            stopTickCount = 0;
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
