package me.kall.narutoloading.core;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.NarutoLoadingClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MouseChecker {
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;

    private static int stopTickCount = 0;
    public static float fadeAlpha = 1.0F;

    public static boolean shouldFade() {
        return stopTickCount >= 20 * 5;
    }

    public static boolean transparency() {
        return fadeAlpha == 0.0F;
    }

    public static final String EMPTY_STRING = "";
    public static final Component EMPTY = Component.empty();

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && NarutoLoadingClient.RENDERER.isRunning()) {
            MouseHandler mouseHandler = Minecraft.getInstance().mouseHandler;

            double x = mouseHandler.xpos();
            double y = mouseHandler.ypos();

            if (Double.isNaN(lastMouseX) || Double.isNaN(lastMouseY)) {
                lastMouseX = x;
                lastMouseY = y;
                return;
            }

            if (lastMouseX == x && lastMouseY == y) {
                stopTickCount++;
            } else {
                stopTickCount = 0;
            }

            lastMouseX = x;
            lastMouseY = y;

            if (shouldFade()) {
                fadeAlpha = Math.max(0.0F, fadeAlpha - 0.05F);
            } else {
                fadeAlpha = Math.min(1.0F, fadeAlpha + 0.05F);
            }
        }
    }

    public static int modifyAlpha(int color) {
        if (!shouldFade()) return color;
        if (fadeAlpha == 0.0F) return (color & 0x00FFFFFF);
        int alpha = (int)(fadeAlpha * 255.0F) & 0xFF;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
