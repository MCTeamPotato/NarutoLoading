package me.kall.narutoloading.core.detection;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.data.NarutoConfig;
import me.kall.narutoloading.data.SourceRoller;
import me.kall.narutoloading.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

public final class KeyChecker {

    private boolean reloadable = false;
    private int reloadCooldown = 0;

    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();

        if (!NarutoRenderer.INSTANCE.isRunning() || minecraft.level != null) {
            reset();
            return;
        }

        if (reloadCooldown > 0) {
            reloadCooldown--;
            return;
        }

        if (SourcesSelectionScreen.screenTriggerable != 0) return;
        if (SourceRoller.sourceRollable != 0) return;

        long window = minecraft.getWindow().getWindow();
        int state = GLFW.glfwGetKey(window, NarutoConfig.reload);

        if (state == GLFW.GLFW_PRESS) {
            reloadCooldown = 20;
            reloadable = true;
        }
    }

    private void reset() {
        reloadable = false;
        reloadCooldown = 0;
    }

    public void reload(NarutoRenderer renderer) {
        if (this.reloadable) {
            this.reloadable = false;
            renderer.shutdown();
            renderer.setup();
            NarutoLoading.LOGGER.info("NarutoRenderer reloads successfully.");
        }
    }
}