package me.kall.narutoloading.noworld.core.checker;

import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import me.kall.narutoloading.noworld.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class KeyChecker {
    private boolean reloadable = false;
    private int interval = 0;
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

        if (this.interval > 0) {
            this.interval--;
            return;
        }

        if (SourcesSelectionScreen.Trigger.interval != 0) return;

        long window = minecraft.getWindow().getWindow();
        int state = GLFW.glfwGetKey(window, BaseEnv.narutoConfig.reload);

        if (state == GLFW.GLFW_PRESS) {
            this.interval = 20;
            this.reloadable = true;
        }
    }

    private void reset() {
        this.reloadable = false;
        this.interval = 0;
    }

    public void reload() {
        if (!this.reloadable) return;
        this.reloadable = false;
        this.renderer.shutdown();
        this.renderer.setup();
    }
}