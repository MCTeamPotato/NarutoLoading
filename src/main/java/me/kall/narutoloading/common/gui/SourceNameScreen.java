package me.kall.narutoloading.common.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.noworld.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class SourceNameScreen extends EmptiableEditBoxes {
    private final Screen lastScreen;
    private final String videoUrl;
    private final Consumer<String> onConfirm;
    private EditBox nameBox;

    public static final Component TITLE = new TranslatableComponent("screen.narutoloading.name");
    public static final Component NAME_LABEL = new TranslatableComponent("box.narutoloading.folder_name");
    public static final Component HINT = new TranslatableComponent("hint.narutoloading.folder_name");

    public SourceNameScreen(Screen lastScreen, String videoUrl, Consumer<String> onConfirm) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.videoUrl = videoUrl;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int boxWidth = 200;
        int boxHeight = 20;

        int totalHeight = boxHeight * 4 + 50;
        int startY = Math.max(40, (this.height - totalHeight) / 2);

        this.nameBox = new EditBox(this.font, centerX - boxWidth / 2, startY + 40, boxWidth, boxHeight, NAME_LABEL);
        this.nameBox.setMaxLength(1024);
        this.nameBox.setValue(NarutoLoading.BLANK);
        this.nameBox.setFilter(this::isValidFolderName);
        this.addWidget(this.nameBox);

        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonY = startY + 100;

        this.addButton(new Button(centerX - buttonWidth - 5, buttonY, buttonWidth, buttonHeight, SourcesSelectionScreen.DONE, button -> onDone()));
        this.addButton(new Button(centerX + 5, buttonY, buttonWidth, buttonHeight, SourcesSelectionScreen.CANCEL, button -> onCancel()));
        this.setInitialFocus(this.nameBox);
    }

    private boolean isValidFolderName(@NotNull String name) {
        if (name.isEmpty()) return true;
        return name.matches("[a-zA-Z0-9_-]+");
    }

    private void onDone() {
        String folderName = this.nameBox.getValue().trim();
        if (!folderName.isEmpty()) {
            this.onConfirm.accept(folderName);
            Minecraft.getInstance().setScreen(this.lastScreen);
        }
    }

    private void onCancel() {
        Minecraft.getInstance().setScreen(this.lastScreen);
    }

    @Override
    public void tick() {
        super.tick();
        this.nameBox.tick();
    }

    @Override
    public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        this.font.draw(graphics, TITLE, centerX, 20, 0xFFFFFF);
        this.font.draw(graphics, NAME_LABEL, centerX, this.nameBox.y - 12, 0xFFFFFF);

        this.font.draw(graphics, HINT, centerX, this.nameBox.y + 30, 0xAAAAAA);

        if (!this.nameBox.getValue().isEmpty() && !isValidFolderName(this.nameBox.getValue())) {
            Component error = new TranslatableComponent("error.narutoloading.invalid_folder_name");
            this.font.draw(graphics, error, centerX, this.nameBox.y + 45, 0xFF5555);
        }

        this.nameBox.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.lastScreen);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}