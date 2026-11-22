package me.kall.narutoloading.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import me.kall.narutoloading.bootstrap.Integration;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class NarutoKeys {
    public static final KeyMapping OPEN_SCREEN = new KeyMapping("key.narutoloading.open_screen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F12, "key.categories.narutoloading");

    public static final KeyMapping TOGGLE_COMPAT_MODE = new KeyMapping("key.narutoloading.toggle_compat_mode", KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F12, "key.categories.narutoloading");

    private NarutoKeys() {}

    public static void register(@NotNull RegisterKeyMappingsEvent event) {
        if (Integration.NARUTO_UI) event.register(OPEN_SCREEN);
        event.register(TOGGLE_COMPAT_MODE);
    }
}
