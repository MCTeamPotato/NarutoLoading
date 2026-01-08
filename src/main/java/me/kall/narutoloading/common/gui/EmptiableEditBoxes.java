package me.kall.narutoloading.common.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutoloading.NarutoLoading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class EmptiableEditBoxes extends Screen {
    protected EmptiableEditBoxes(Component title) {
        super(title);
    }

    @Override
    public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partialTick) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean stateLeftShift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
        boolean stateRightShift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean stateRightMouse = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (stateRightMouse && (stateRightShift || stateLeftShift)) {
            for (GuiEventListener renderable : this.children()) {
                if (renderable instanceof EditBox && ((EditBox)renderable).isFocused()) {
                    ((EditBox)renderable).setValue(NarutoLoading.BLANK);
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
