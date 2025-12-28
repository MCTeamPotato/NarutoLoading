package me.kall.narutoloading.noworld.core.checker;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class KeyChecker {
    private boolean reloadable = false;
    private int reloadCooldown = 0;
    private final NarutoRenderer renderer;

    public KeyChecker(NarutoRenderer renderer) {
        this.renderer = renderer;
    }

    public void clientTick(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();

        if (!this.renderer.isRunning() || !this.renderer.isEnabled()) {
            reset();
            return;
        }

        if (this.reloadCooldown > 0) {
            this.reloadCooldown--;
            return;
        }

        if (SourcesSelectionScreen.Trigger.screenTriggerable != 0) return;

        long window = minecraft.getWindow().getWindow();
        int state = GLFW.glfwGetKey(window, BaseEnv.narutoConfig.reload);

        if (state == GLFW.GLFW_PRESS) {
            this.reloadCooldown = 20;
            this.reloadable = true;
        }
    }

    private void reset() {
        this.reloadable = false;
        this.reloadCooldown = 0;
    }

    public void reload() {
        if (!this.reloadable) return;
        this.reloadable = false;
        this.renderer.shutdown();
        this.renderer.setup();
        NarutoLoading.LOGGER.info("NarutoRenderer reloads successfully.");
    }
}