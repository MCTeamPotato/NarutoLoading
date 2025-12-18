package me.kall.narutoloading.gui;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.data.NarutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public class SourcesSelectionScreen extends Screen {
    private final Screen lastScreen;

    private EditBox videoBox;
    private EditBox audioBox;

    private static final Component SCREEN = Component.translatable("screen.narutoloading.selection");
    private static final Component DONE = Component.translatable("button.narutoloading.done");
    private static final Component CANCEL = Component.translatable("button.narutoloading.cancel");
    private static final Component VIDEO = Component.translatable("box.narutoloading.video");
    private static final Component AUDIO = Component.translatable("box.narutoloading.audio");

    public SourcesSelectionScreen(Screen lastScreen) {
        super(SCREEN);
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int boxWidth = 200;
        int boxHeight = 20;
        int spacing = 28;

        this.videoBox = new EditBox(this.font, centerX - boxWidth / 2, centerY - spacing - boxHeight, boxWidth, boxHeight, VIDEO);
        this.videoBox.setMaxLength(256);
        this.videoBox.setValue(NarutoConfig.videoName);
        this.addRenderableWidget(this.videoBox);

        this.audioBox = new EditBox(this.font, centerX - boxWidth / 2, centerY + spacing, boxWidth, boxHeight, AUDIO);
        this.audioBox.setMaxLength(256);
        this.audioBox.setValue(NarutoConfig.audioName);
        this.addRenderableWidget(this.audioBox);

        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonY = centerY + spacing * 2 + 10;

        Button done = Button.builder(DONE, button -> onDone()).bounds(centerX - buttonWidth - 5, buttonY, buttonWidth, buttonHeight).build();
        Button cancel = Button.builder(CANCEL, button -> Minecraft.getInstance().setScreen(this.lastScreen)).bounds(centerX + 5, buttonY, buttonWidth, buttonHeight).build();

        this.addRenderableWidget(done);
        this.addRenderableWidget(cancel);

        this.setInitialFocus(this.videoBox);
    }

    @Override
    public void tick() {
        super.tick();
        this.videoBox.tick();
        this.audioBox.tick();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        graphics.drawCenteredString(this.font, VIDEO, centerX, this.videoBox.getY() - 12, 0xFFFFFF);
        graphics.drawCenteredString(this.font, AUDIO, centerX, this.audioBox.getY() - 12, 0xFFFFFF);
    }

    private void onDone() {
        String video = this.videoBox.getValue();
        String audio = this.audioBox.getValue();
        NarutoConfig.config.put("videoFileName", video).put("audioFileName", audio).saveToFile();
        NarutoConfig.init();
        NarutoRenderer.INSTANCE.shutdown();
        NarutoRenderer.INSTANCE.setup();
        Minecraft.getInstance().setScreen(this.lastScreen);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.lastScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static int screenTriggerable = 0;

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getInstance();

        if (screenTriggerable > 0) {
            screenTriggerable--;
            return;
        }

        long window = mc.getWindow().getWindow();
        int state = GLFW.glfwGetKey(window, NarutoConfig.reload);
        int stateCtrl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL);

        if (state == GLFW.GLFW_PRESS && stateCtrl == GLFW.GLFW_PRESS && !(mc.screen instanceof SourcesSelectionScreen)) {
            screenTriggerable = 200;
            mc.setScreen(new SourcesSelectionScreen(mc.screen));
        }
    }
}
