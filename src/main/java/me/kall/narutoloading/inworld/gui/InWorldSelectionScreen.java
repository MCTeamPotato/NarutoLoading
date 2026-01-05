package me.kall.narutoloading.inworld.gui;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.common.env.ytdlp.YtDlpDownloader;
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
import me.kall.narutoloading.noworld.gui.SourceNameScreen;
import me.kall.narutoloading.noworld.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class InWorldSelectionScreen extends SourcesSelectionScreen {
    private final NarutoInWorldRenderer renderer;
    private Checkbox localSoundCheck;
    private Checkbox hideInnerCheck;

    private EditBox widthBox;
    private EditBox heightBox;

    public static final Component LOCAL_SOUND = Component.translatable("box.narutoloading.local_sound");
    public static final Component HIDE_INNER = Component.translatable("box.narutoloading.hide_inner");
    public static final Component VIDEO_WIDTH = Component.translatable("box.narutoloading.video_width");
    public static final Component VIDEO_HEIGHT = Component.translatable("box.narutoloading.video_height");

    public static final Component CLEAR = Component.translatable("button.narutoloading.clear");

    public InWorldSelectionScreen(Screen lastScreen, NarutoInWorldRenderer renderer) {
        super(lastScreen);
        this.renderer = renderer;
    }

    protected void editBoxes(int centerX, int boxWidth, int boxHeight, int editBoxSpacing) {
        this.widthBox = new EditBox(this.font, centerX - boxWidth / 2, this.currentY, 95, boxHeight, VIDEO_WIDTH);
        this.widthBox.setMaxLength(5);
        this.widthBox.setValue(String.valueOf(this.renderer.screen.videoWidth()));
        this.widthBox.setFilter(this::valid);
        this.addRenderableWidget(this.widthBox);

        this.heightBox = new EditBox(this.font, centerX + 5, this.currentY, 95, boxHeight, VIDEO_HEIGHT);
        this.heightBox.setMaxLength(5);
        this.heightBox.setValue(String.valueOf(this.renderer.screen.videoHeight()));
        this.heightBox.setFilter(this::valid);
        this.addRenderableWidget(this.heightBox);

        this.currentY += boxHeight + editBoxSpacing;
    }

    protected void checkBoxes(int centerX, int boxHeight) {
        int checkWidth = 200;
        int checkBoxSpacing = 10;

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
        return this.renderer.screen.relativeVideoPath(BaseEnv.narutoConfig.videoFileName);
    }

    protected String initAudio() {
        return this.renderer.screen.relativeAudioPath(BaseEnv.narutoConfig.audioFileName);
    }

    private boolean valid(@NotNull String value) {
        if (value.isEmpty()) return true;
        try {
            int num = Integer.parseInt(value);
            return num > 0 && num <= 7680;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int width() {
        try {
            int width = Integer.parseInt(this.widthBox.getValue());
            return width > 0 ? width : 1280;
        } catch (NumberFormatException e) {
            return 1280;
        }
    }

    private int height() {
        try {
            int height = Integer.parseInt(this.heightBox.getValue());
            return height > 0 ? height : 720;
        } catch (NumberFormatException e) {
            return 720;
        }
    }

    @Override
    protected void onDone() {
        String videoFilename = this.videoBox.getValue();
        String audioFileName = this.audioBox.getValue();

        if (videoFilename.startsWith("http")) {
            Minecraft.getInstance().setScreen(new SourceNameScreen(this.lastScreen, videoFilename, folderName -> handleUrlDownload(videoFilename, audioFileName, this.renderer.screen, folderName)));
        } else {
            handleLocalFiles();
            Minecraft.getInstance().setScreen(this.lastScreen);
        }
    }

    private void handleLocalFiles() {
        this.renderer.screen.setPath(this.videoBox.getValue(), this.audioBox.getValue().isBlank() ? this.videoBox.getValue() : this.audioBox.getValue());
        this.renderer.screen.setSize(this.width(), this.height());
        this.renderer.screen.setHideInner(this.hideInnerCheck.selected());

        if (this.renderer.screen.hideInner()) {
            ClientScreensRenderer.HIDDEN_DISPLAYERS.computeIfAbsent(this.renderer.screen.dimension(), key -> new LongOpenHashSet()).addAll(this.renderer.screen.areaInvolved());
        } else {
            LongSet hiddenAreas = ClientScreensRenderer.HIDDEN_DISPLAYERS.get(this.renderer.screen.dimension());
            if (hiddenAreas != null) {
                hiddenAreas.removeAll(this.renderer.screen.areaInvolved());
            }
        }

        Minecraft.getInstance().levelRenderer.allChanged();

        this.renderer.shutdown();

        if (this.localSoundCheck.selected()) {
            setupLocalSound(this.renderer.screen);
        } else {
            this.renderer.screen.setLocalSound(InWorldScreen.NO_LOCAL_SOUND);
            this.renderer.setup();
        }

        NarutoPackets.INSTANCE.sendToServer(new ArgUpdatePacket(this.renderer.screen));
    }

    private void handleUrlDownload(String videoUrl, String audioUrl, InWorldScreen inWorldScreen, String folderName) {
        Path outputDir = YtDlpDownloader.getDefaultOutputDir(folderName);

        NarutoLoading.LOGGER.info("{}Starting URL download with folder name: {}", NarutoLoading.info(), folderName);

        CompletableFuture<Void> videoFuture = YtDlpDownloader.download(BaseEnv.ytDlpProvider.absoluteYtDlp, videoUrl, outputDir, "video", YtDlpDownloader.DownloadType.VIDEO, progress -> NarutoLoading.LOGGER.info("{}Video download progress: {}", NarutoLoading.info(), progress), downloadResult -> {
                    NarutoLoading.LOGGER.info("{}Video download of {} processed. {}", NarutoLoading.info(), videoUrl, downloadResult.toString());
                    if (downloadResult.success() && downloadResult.hasVideo()) {
                        Minecraft.getInstance().execute(() -> {
                            String relativePath = NarutoConfig.relative(downloadResult.videoPath());
                            inWorldScreen.setPath(relativePath, relativePath);
                            NarutoLoading.LOGGER.info("{}Set video path to: {}", NarutoLoading.info(), relativePath);
                        });
                    }
                }
        );

        if (videoFuture != null) {
            videoFuture.thenRun(() -> {
                String audioUrlToUse = audioUrl.isBlank() ? videoUrl : audioUrl;
                if (audioUrlToUse.startsWith("http")) {
                    CompletableFuture<Void> audioFuture = YtDlpDownloader.download(BaseEnv.ytDlpProvider.absoluteYtDlp, audioUrlToUse, outputDir, "audio", YtDlpDownloader.DownloadType.AUDIO, progress -> NarutoLoading.LOGGER.info("{}Audio download progress: {}", NarutoLoading.info(), progress), downloadResult -> {
                                NarutoLoading.LOGGER.info("{}Audio download of {} processed. {}", NarutoLoading.info(), audioUrlToUse, downloadResult.toString());
                                if (downloadResult.success() && downloadResult.hasAudio()) {
                                    Minecraft.getInstance().execute(() -> {
                                        String relativePath = NarutoConfig.relative(downloadResult.audioPath());
                                        String currentVideo = inWorldScreen.relativeVideoPath(NarutoLoading.BLANK);
                                        inWorldScreen.setPath(currentVideo, relativePath);
                                        NarutoLoading.LOGGER.info("{}Set audio path to: {}", NarutoLoading.info(), relativePath);
                                    });
                                }
                            }
                    );

                    if (audioFuture != null) {
                        audioFuture.thenRun(() -> finalizeUrlSetup(inWorldScreen));
                    }
                } else {
                    finalizeUrlSetup(inWorldScreen);
                }
            });
        }
    }

    private void finalizeUrlSetup(InWorldScreen inWorldScreen) {
        Minecraft.getInstance().execute(() -> {
            inWorldScreen.setSize(this.width(), this.height());

            inWorldScreen.setHideInner(this.hideInnerCheck.selected());

            if (inWorldScreen.hideInner()) {
                ClientScreensRenderer.HIDDEN_DISPLAYERS.computeIfAbsent(inWorldScreen.dimension(), key -> new LongOpenHashSet()).addAll(inWorldScreen.areaInvolved());
            } else {
                LongSet hiddenAreas = ClientScreensRenderer.HIDDEN_DISPLAYERS.get(inWorldScreen.dimension());
                if (hiddenAreas != null) {
                    hiddenAreas.removeAll(inWorldScreen.areaInvolved());
                }
            }

            this.renderer.shutdown();

            if (this.localSoundCheck.selected()) {
                setupLocalSound(inWorldScreen);
            } else {
                inWorldScreen.setLocalSound(InWorldScreen.NO_LOCAL_SOUND);
                this.renderer.setup();
            }

            NarutoPackets.INSTANCE.sendToServer(new ArgUpdatePacket(this.renderer.screen));

            NarutoLoading.LOGGER.info("{}URL download and setup completed for in-world screen: {}", NarutoLoading.info(), inWorldScreen.toString());
        });
    }

    private void setupLocalSound(@NotNull InWorldScreen inWorldScreen) {
        inWorldScreen.setLocalSound(InWorldScreen.HAS_LOCAL_SOUND);
        AudioConverter audioConverter = new AudioConverter(NarutoConfig.absolute(inWorldScreen.relativeAudioPath(NarutoLoading.BLANK)), BaseEnv.ffmpegProvider.absoluteFFmpeg, BaseEnv.ffmpegProvider.absoluteFFprobe);
        audioConverter.setup(() -> {
            ResourceZipGenerator resourceZipGenerator = new ResourceZipGenerator(audioConverter.converted);
            resourceZipGenerator.generate();
            resourceZipGenerator.reload(this.renderer);
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (this.widthBox != null) this.widthBox.tick();
        if (this.heightBox != null) this.heightBox.tick();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.widthBox != null && this.heightBox != null) {
            int centerX = this.width / 2;
            graphics.drawString(this.font, VIDEO_WIDTH, centerX - 95, this.widthBox.getY() - 12, 0xFFFFFF);
            graphics.drawString(this.font, VIDEO_HEIGHT, centerX + 10, this.heightBox.getY() - 12, 0xFFFFFF);

            int width = width();
            int height = height();
            String ratio = String.format("%.2f:1", (float)width / height);
            graphics.drawCenteredString(this.font, Component.literal(ratio), centerX, this.widthBox.getY() + 25, 0xAAAAAA);
        }
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