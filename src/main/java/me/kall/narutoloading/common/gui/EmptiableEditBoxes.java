package me.kall.narutoloading.common.gui;

import me.kall.narutoloading.NarutoLoading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class EmptiableEditBoxes extends Screen {
    protected EmptiableEditBoxes(Component title) {
        super(title);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long window = Minecraft.getInstance().getWindow().handle();
        boolean stateLeftShift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
        boolean stateRightShift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean stateRightMouse = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (stateRightMouse && (stateRightShift || stateLeftShift)) {
            for (Renderable renderable : this.renderables) {
                if (renderable instanceof EditBox editBox && editBox.isFocused()) {
                    editBox.setValue(NarutoLoading.BLANK);
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
