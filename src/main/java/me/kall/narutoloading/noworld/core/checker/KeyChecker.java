package me.kall.narutoloading.noworld.core.checker;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import me.kall.narutoloading.noworld.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public final class KeyChecker {
    private static boolean reloadable = false;
    private static int interval = 0;

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (!NarutoRenderer.INSTANCE.isRunning() || !NarutoRenderer.INSTANCE.isEnabled()) {
            reset();
            return;
        }

        if (interval > 0) {
            interval--;
            return;
        }

        if (SourcesSelectionScreen.Trigger.interval != 0) return;

        long window = minecraft.getWindow().getWindow();
        int state = GLFW.glfwGetKey(window, BaseEnv.narutoConfig.reload);

        if (state == GLFW.GLFW_PRESS) {
            interval = 20;
            reloadable = true;
        }
    }

    private static void reset() {
        reloadable = false;
        interval = 0;
    }

    public void reload() {
        if (!reloadable) return;
        reloadable = false;
        NarutoRenderer.INSTANCE.shutdown();
        NarutoRenderer.INSTANCE.setup();
    }
}