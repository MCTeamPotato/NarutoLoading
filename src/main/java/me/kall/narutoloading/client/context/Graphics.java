package me.kall.narutoloading.client.context;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

public final class Graphics {
    private static GuiGraphics captured;

    public static void set(GuiGraphics graphics) {
        captured = graphics;
    }

    public static @Nullable GuiGraphics get() {
        return captured;
    }
}
