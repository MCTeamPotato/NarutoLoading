package me.kall.narutoloading.inworld.gui;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import me.kall.narutoloading.inworld.network.ArgUpdatePacket;
import me.kall.narutoloading.inworld.network.SourceSelectionPacket;
import me.kall.narutoloading.noworld.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class InWorldSelectionScreen extends SourcesSelectionScreen {
    private final ClientScreensRenderer.ClientScreen clientScreen;

    public InWorldSelectionScreen(Screen lastScreen, ClientScreensRenderer.ClientScreen clientScreen) {
        super(lastScreen);
        this.clientScreen = clientScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int boxWidth = 200;
        int boxHeight = 20;
        int spacing = 28;

        this.videoBox = new EditBox(this.font, centerX - boxWidth / 2, centerY - spacing - boxHeight, boxWidth, boxHeight, VIDEO);
        this.videoBox.setMaxLength(1024);
        this.videoBox.setValue(NarutoConfig.relative(this.clientScreen.screen().absoluteVideoPath(BaseEnv.narutoConfig.absoluteVideoPath)));
        this.addRenderableWidget(this.videoBox);

        this.audioBox = new EditBox(this.font, centerX - boxWidth / 2, centerY + spacing, boxWidth, boxHeight, AUDIO);
        this.audioBox.setMaxLength(1024);
        this.audioBox.setValue(NarutoConfig.relative(this.clientScreen.screen().absoluteAudioPath(BaseEnv.narutoConfig.absoluteAudioPath)));
        this.addRenderableWidget(this.audioBox);

        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonY = centerY + spacing * 2 + 10;

        Button done = Button.builder(DONE, button -> onDone()).bounds(centerX - buttonWidth - 5, buttonY, buttonWidth, buttonHeight).build();
        Button cancel = Button.builder(CANCEL, button -> onCancel()).bounds(centerX + 5, buttonY, buttonWidth, buttonHeight).build();

        this.addRenderableWidget(done);
        this.addRenderableWidget(cancel);

        this.setInitialFocus(this.videoBox);
    }

    @Override
    protected void onDone() {
        String videoFilename = NarutoConfig.absolute(this.videoBox.getValue());
        String audioFileName = NarutoConfig.absolute(this.audioBox.getValue());
        this.clientScreen.screen().set(videoFilename, audioFileName.isBlank() ? videoFilename : audioFileName);
        NarutoPackets.INSTANCE.sendToServer(new ArgUpdatePacket(this.clientScreen.screen()));
        this.clientScreen.renderer().shutdown();
        this.clientScreen.renderer().setup();
        Minecraft.getInstance().setScreen(this.lastScreen);
    }

    @Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID)
    public static final class Trigger {
        private static int screenTriggerable = 0;

        @SubscribeEvent
        public static void serverTick(TickEvent.@NotNull ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                if (screenTriggerable > 0) screenTriggerable--;
            }
        }

        @SubscribeEvent
        public static void rightClickScreen(PlayerInteractEvent.@NotNull RightClickBlock event) {
            BlockPos pos = event.getPos();
            if (event.getLevel() instanceof ServerLevel level && Displayers.isDisplayer(level, pos.asLong())) {
                if (screenTriggerable > 0) return;
                NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new SourceSelectionPacket(pos.asLong()));
                screenTriggerable = 20;
            }
        }
    }
}
