package me.kall.narutoloading.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class NarutoKeys {
    public static final KeyMapping OPEN_SCREEN = new KeyMapping("key.narutoloading.open_screen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F12, "key.categories.narutoloading");

    private NarutoKeys() {}

    public static void register(@NotNull RegisterKeyMappingsEvent event) {
        event.register(OPEN_SCREEN);
    }
}
