package me.kall.narutoloading.inworld.gui;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.gui.util.AudioConverter;
import me.kall.narutoloading.inworld.gui.util.ResourceZipGenerator;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import me.kall.narutoloading.inworld.network.ArgUpdatePacket;
import me.kall.narutoloading.inworld.network.ClearScreenPacket;
import me.kall.narutoloading.inworld.network.SourceSelectionPacket;
import me.kall.narutoloading.noworld.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class InWorldSelectionScreen extends SourcesSelectionScreen {
    private final NarutoInWorldRenderer renderer;
    private Checkbox cullableCheck;
    private Checkbox localSoundCheck;
    private Checkbox hideInnerCheck;

    public static final Component CULLABLE = Component.translatable("box.narutoloading.cullable");
    public static final Component LOCAL_SOUND = Component.translatable("box.narutoloading.local_sound");
    public static final Component HIDE_INNER = Component.translatable("box.narutoloading.hide_inner");

    public static final Component CLEAR = Component.translatable("button.narutoloading.clear");

    public InWorldSelectionScreen(Screen lastScreen, NarutoInWorldRenderer renderer) {
        super(lastScreen);
        this.renderer = renderer;
    }

    protected void checkBoxes(int centerX, int boxHeight) {
        int checkWidth = 200;
        int checkBoxSpacing = 10;

        this.cullableCheck = new Checkbox(centerX - checkWidth / 2, this.currentY, checkWidth, boxHeight, CULLABLE, this.renderer.screen.isCullable());
        this.addRenderableWidget(this.cullableCheck);
        this.currentY += boxHeight + checkBoxSpacing;

        this.localSoundCheck = new Checkbox(centerX - checkWidth / 2, this.currentY, checkWidth, boxHeight, LOCAL_SOUND, this.renderer.screen.isLocalSound());
        this.addRenderableWidget(this.localSoundCheck);
        this.currentY += boxHeight + checkBoxSpacing;

        this.hideInnerCheck = new Checkbox(centerX - checkWidth / 2, this.currentY, checkWidth, boxHeight, HIDE_INNER, this.renderer.screen.hideInner());
        this.addRenderableWidget(this.hideInnerCheck);
        this.currentY += boxHeight + checkBoxSpacing;
    }

    protected void buttons(int centerX, int buttonWidth, int buttonHeight) {
        Button random = Button.builder(RANDOM, button -> onRandom()).bounds(centerX - buttonWidth - 5, this.currentY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(random);

        Button clear = Button.builder(CLEAR, button -> NarutoPackets.INSTANCE.sendToServer(new ClearScreenPacket(this.renderer.screen))).bounds(centerX + 5, this.currentY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(clear);
        this.currentY += buttonHeight + 5;
    }

    protected String initVideo() {
        return this.renderer.screen.relativeVideoPath(NarutoConfig.relative(BaseEnv.narutoConfig.absoluteVideoPath));
    }

    protected String initAudio() {
        return this.renderer.screen.relativeAudioPath(NarutoConfig.relative(BaseEnv.narutoConfig.absoluteAudioPath));
    }

    @Override
    protected void onDone() {
        String videoFilename = this.videoBox.getValue();
        String audioFileName = this.audioBox.getValue();
        InWorldScreen inWorldScreen = this.renderer.screen;

        inWorldScreen.set(videoFilename, audioFileName.isBlank() ? videoFilename : audioFileName);

        inWorldScreen.setCullable(this.cullableCheck.selected());
        inWorldScreen.setHideInner(this.hideInnerCheck.selected());

        if (inWorldScreen.hideInner()) {
            ClientScreensRenderer.HIDDEN_DISPLAYERS.computeIfAbsent(inWorldScreen.dimension(), key -> new LongOpenHashSet()).addAll(inWorldScreen.areaInvolved());
        } else {
            LongSet hiddenAreas = ClientScreensRenderer.HIDDEN_DISPLAYERS.get(inWorldScreen.dimension());
            if (hiddenAreas != null) {
                hiddenAreas.removeAll(inWorldScreen.areaInvolved());
            }
        }

        Minecraft.getInstance().levelRenderer.allChanged();

        this.renderer.shutdown();

        if (this.localSoundCheck.selected()) {
            this.renderer.screen.setLocalSound(InWorldScreen.HAS_LOCAL_SOUND);
            AudioConverter audioConverter = new AudioConverter(inWorldScreen.relativeAudioPath(NarutoLoading.BLANK), BaseEnv.ffmpegProvider.absoluteFFmpeg);
            audioConverter.setup(() -> {
                ResourceZipGenerator resourceZipGenerator = new ResourceZipGenerator(audioConverter.converted);
                resourceZipGenerator.generate();
                resourceZipGenerator.reload(this.renderer);
            });
        } else {
            this.renderer.screen.setLocalSound(InWorldScreen.NO_LOCAL_SOUND);
        }

        NarutoPackets.INSTANCE.sendToServer(new ArgUpdatePacket(this.renderer.screen));
        Minecraft.getInstance().setScreen(this.lastScreen);
    }

    @Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID)
    public static final class Trigger {
        private static int interval = 0;

        @SubscribeEvent
        public static void serverTick(TickEvent.@NotNull ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                if (interval > 0) interval--;
            }
        }

        @SubscribeEvent
        public static void rightClickScreen(PlayerInteractEvent.@NotNull RightClickBlock event) {
            BlockPos pos = event.getPos();
            if (event.getLevel() instanceof ServerLevel level && Displayers.isDisplayer(level, pos.asLong())) {
                if (interval > 0) return;
                NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new SourceSelectionPacket(pos.asLong()));
                interval = 20;
            }
        }
    }
}