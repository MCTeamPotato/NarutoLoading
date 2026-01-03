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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

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
        return this.renderer.screen.relativeVideoPath(BaseEnv.narutoConfig.videoFileName);
    }

    protected String initAudio() {
        return this.renderer.screen.relativeAudioPath(BaseEnv.narutoConfig.audioFileName);
    }

    @Override
    protected void onDone() {
        String videoFilename = this.videoBox.getValue();
        String audioFileName = this.audioBox.getValue();
        InWorldScreen inWorldScreen = this.renderer.screen;

        if (videoFilename.startsWith("http")) {
            Minecraft.getInstance().setScreen(new SourceNameScreen(this.lastScreen, videoFilename, folderName -> handleUrlDownload(videoFilename, audioFileName, inWorldScreen, folderName)));
        } else {
            handleLocalFiles(videoFilename, audioFileName, inWorldScreen);
            Minecraft.getInstance().setScreen(this.lastScreen);
        }
    }

    private void handleLocalFiles(String videoFilename, @NotNull String audioFileName, @NotNull InWorldScreen inWorldScreen) {
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
            setupLocalSound(inWorldScreen);
        } else {
            inWorldScreen.setLocalSound(InWorldScreen.NO_LOCAL_SOUND);
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
                            inWorldScreen.set(relativePath, relativePath);
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
                                        inWorldScreen.set(currentVideo, relativePath);
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
        AudioConverter audioConverter = new AudioConverter(inWorldScreen.relativeAudioPath(NarutoLoading.BLANK), BaseEnv.ffmpegProvider.absoluteFFmpeg);
        audioConverter.setup(() -> {
            ResourceZipGenerator resourceZipGenerator = new ResourceZipGenerator(audioConverter.converted);
            resourceZipGenerator.generate();
            resourceZipGenerator.reload(this.renderer);
        });
    }

    @Contract(pure = true)
    private static @NotNull String extractLetters(@NotNull String input) {
        return input.replaceAll("[^A-Za-z]", "");
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
                NarutoPackets.INSTANCE.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), new SourceSelectionPacket(pos.asLong()));
                interval = 20;
            }
        }
    }
}