package me.kall.narutoloading.core.detection;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public final class KeyChecker {

    private static boolean reloadable = false;
    private static int reloadCooldown = 0;

    private static final int COOLDOWN_TICKS = 200;

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft minecraft = Minecraft.getInstance();

        if (!NarutoRenderer.INSTANCE.isRunning() || minecraft.level != null) {
            reset();
            return;
        }

        if (reloadCooldown > 0) {
            reloadCooldown--;
            return;
        }

        long window = minecraft.getWindow().getWindow();
        int state = GLFW.glfwGetKey(window, NarutoConfig.RELOAD);

        if (state == GLFW.GLFW_PRESS) {
            reloadCooldown = COOLDOWN_TICKS;
            reloadable = true;
        }
    }

    private static void reset() {
        reloadable = false;
        reloadCooldown = 0;
    }

    public static void reload(NarutoRenderer renderer) {
        if (KeyChecker.reloadable) {
            KeyChecker.reloadable = false;
            renderer.shutdown();
            renderer.setup();
            NarutoLoading.LOGGER.info("NarutoRenderer reloads successfully.");
        }
    }
}