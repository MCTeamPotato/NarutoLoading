package me.kall.narutoloading.inworld.gui;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.common.env.config.SourceCollector;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.gui.util.AudioConverter;
import me.kall.narutoloading.inworld.gui.util.ResourceZipGenerator;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import me.kall.narutoloading.inworld.network.ArgUpdatePacket;
import me.kall.narutoloading.inworld.network.SourceSelectionPacket;
import me.kall.narutoloading.noworld.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
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

    public static final Component RANDOM = Component.translatable("button.narutoloading.random");

    public InWorldSelectionScreen(Screen lastScreen, NarutoInWorldRenderer renderer) {
        super(lastScreen);
        this.renderer = renderer;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int boxWidth = 200;
        int boxHeight = 20;
        int spacing = 28;

        this.videoBox = new EditBox(this.font, centerX - boxWidth / 2, centerY - spacing * 2 - boxHeight, boxWidth, boxHeight, VIDEO);
        this.videoBox.setMaxLength(1024);
        this.videoBox.setValue(this.renderer.screen.relativeVideoPath(NarutoConfig.relative(BaseEnv.narutoConfig.absoluteVideoPath)));
        this.addRenderableWidget(this.videoBox);

        this.audioBox = new EditBox(this.font, centerX - boxWidth / 2, centerY - spacing, boxWidth, boxHeight, AUDIO);
        this.audioBox.setMaxLength(1024);
        this.audioBox.setValue(this.renderer.screen.relativeAudioPath(NarutoConfig.relative(BaseEnv.narutoConfig.absoluteAudioPath)));
        this.addRenderableWidget(this.audioBox);

        int firstCheckY = centerY + spacing / 2;
        int checkBoxSpacing = 25;
        int checkWidth = 200;

        this.cullableCheck = new Checkbox(centerX - checkWidth / 2, firstCheckY, checkWidth, boxHeight, CULLABLE, this.renderer.screen.isCullable());
        this.addRenderableWidget(this.cullableCheck);

        this.localSoundCheck = new Checkbox(centerX - checkWidth / 2, firstCheckY + checkBoxSpacing, checkWidth, boxHeight, LOCAL_SOUND, this.renderer.screen.isLocalSound());
        this.addRenderableWidget(this.localSoundCheck);

        this.hideInnerCheck = new Checkbox(centerX - checkWidth / 2, firstCheckY + checkBoxSpacing * 2, checkWidth, boxHeight, HIDE_INNER, this.renderer.screen.hideInner());
        this.addRenderableWidget(this.hideInnerCheck);

        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonY = centerY + spacing * 2 + 50;

        Button random = Button.builder(RANDOM, button -> onRandom()).bounds(centerX - buttonWidth * 3 / 2 - 10, buttonY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(random);

        Button done = Button.builder(DONE, button -> onDone()).bounds(centerX - buttonWidth / 2, buttonY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(done);

        Button cancel = Button.builder(CANCEL, button -> onCancel()).bounds(centerX + buttonWidth / 2 + 10, buttonY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(cancel);

        this.setInitialFocus(this.videoBox);
    }

    private void onRandom() {
        SourceCollector.Source source = SourceCollector.roll();
        if (source != null) {
            String relativeVideo = NarutoConfig.relative(source.absoluteVideoPath());
            String relativeAudio = NarutoConfig.relative(source.absoluteAudioPath());

            this.videoBox.setValue(relativeVideo);
            this.audioBox.setValue(relativeAudio);

            NarutoLoading.LOGGER.info("{}Randomly selected source - Video: {}, Audio: {}", NarutoLoading.info(), relativeVideo, relativeAudio);
        } else {
            NarutoLoading.LOGGER.warn("{}No sources available for random selection", NarutoLoading.info());
        }
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
            AudioConverter audioConverter = new AudioConverter(inWorldScreen.relativeAudioPath(""), BaseEnv.ffmpegProvider.absoluteFFmpeg);
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