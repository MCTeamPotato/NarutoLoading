package me.kall.narutoloading.client.input;

import me.kall.narutoloading.client.ui.NarutoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.glfw.GLFW.*;

public final class PasteListener {
    private static final String MODERN_UI_SCREEN_PACKAGE = "icyllis.modernui.";

    private static int cooldown = 20;

    private PasteListener() {}

    public static void tick() {
        cooldown--;
        if (cooldown > 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().getWindow();

        boolean pressed = (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS) && glfwGetKey(window, GLFW_KEY_V) == GLFW_PRESS;

        if (!pressed) return;

        Screen current = minecraft.screen;
        if (hasTextInput(current)) return;

        String url = clipboardUrl(minecraft);
        if (url == null) return;

        cooldown = 20;

        minecraft.setScreen(NarutoScreen.create(current, url));
    }

    private static boolean hasTextInput(@Nullable Screen screen) {
        if (screen == null) return false;
        if (screen.getFocused() instanceof EditBox) return true;
        return screen.getClass().getName().startsWith(MODERN_UI_SCREEN_PACKAGE);
    }

    private static @Nullable String clipboardUrl(@NotNull Minecraft minecraft) {
        String trimmed = minecraft.keyboardHandler.getClipboard().trim();
        return looksLikeUrl(trimmed) ? trimmed : null;
    }

    private static boolean looksLikeUrl(@NotNull String text) {
        if (!text.startsWith("http://") && !text.startsWith("https://")) return false;

        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) return false;
        }

        return true;
    }
}
