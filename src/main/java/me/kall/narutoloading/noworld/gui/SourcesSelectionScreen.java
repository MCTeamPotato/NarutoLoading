package me.kall.narutoloading.noworld.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.Strings;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.common.env.config.SourceCollector;
import me.kall.narutoloading.common.env.ytdlp.YtDlpDownloader;
import me.kall.narutoloading.common.gui.EmptiableEditBoxes;
import me.kall.narutoloading.common.gui.SourceNameScreen;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class SourcesSelectionScreen extends EmptiableEditBoxes {
    protected final Screen lastScreen;
    protected EditBox videoBox;
    protected EditBox audioBox;
    protected int currentY;

    public static final Component SCREEN = new TranslatableComponent("screen.narutoloading.selection");

    public static final Component DONE = new TranslatableComponent("button.narutoloading.done");
    public static final Component CANCEL = new TranslatableComponent("button.narutoloading.cancel");
    public static final Component RANDOM = new TranslatableComponent("button.narutoloading.random");

    public static final Component VIDEO = new TranslatableComponent("box.narutoloading.video");
    public static final Component AUDIO = new TranslatableComponent("box.narutoloading.audio");

    public SourcesSelectionScreen(Screen lastScreen) {
        super(SCREEN);
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        int boxWidth = 200;
        int boxHeight = 20;
        int editBoxSpacing = 18;

        this.currentY = 15;

        this.videoBox = new EditBox(this.font, centerX - boxWidth / 2, this.currentY, boxWidth, boxHeight, VIDEO);
        this.videoBox.setMaxLength(1024);
        this.videoBox.setValue(this.initVideo());
        this.addWidget(this.videoBox);
        this.currentY += boxHeight + editBoxSpacing;

        this.audioBox = new EditBox(this.font, centerX - boxWidth / 2, this.currentY, boxWidth, boxHeight, AUDIO);
        this.audioBox.setMaxLength(1024);
        this.audioBox.setValue(this.initAudio());
        this.addWidget(this.audioBox);
        this.currentY += boxHeight + editBoxSpacing;

        this.editBoxes(centerX, boxWidth, boxHeight, editBoxSpacing);
        this.currentY += boxHeight + 5;

        this.checkBoxes(centerX, boxHeight);

        int buttonWidth = 80;
        int buttonHeight = 20;

        this.buttons(centerX, buttonWidth, buttonHeight);
        this.addButton(new Button(centerX - buttonWidth - 5, this.currentY, buttonWidth, buttonHeight, DONE, button -> onDone()));
        this.addButton(new Button(centerX + 5, this.currentY, buttonWidth, buttonHeight, CANCEL, button -> onCancel()));
    }

    protected void editBoxes(int centerX, int boxWidth, int boxHeight, int editBoxSpacing) {}

    protected void checkBoxes(int centerX, int boxHeight) {}

    protected void buttons(int centerX, int buttonWidth, int buttonHeight) {
        Button random = new Button(centerX - buttonWidth - 5, this.currentY, buttonWidth * 2 + 10, buttonHeight, RANDOM, button -> onRandom());
        this.addButton(random);
        this.currentY += buttonHeight + 5;
    }

    protected String initVideo() {
        return BaseEnv.narutoConfig.videoFileName;
    }

    protected String initAudio() {
        return BaseEnv.narutoConfig.audioFileName;
    }

    protected void onCancel() {
        Minecraft.getInstance().setScreen(this.lastScreen);
    }

    protected void onDone() {
        String videoFilename = this.videoBox.getValue();
        String audioFileName = this.audioBox.getValue();

        if (videoFilename.startsWith("http")) {
            Minecraft.getInstance().setScreen(new SourceNameScreen(this.lastScreen, folderName -> this.download(videoFilename, audioFileName, folderName)));
        } else {
            this.handleLocalFiles();
            Minecraft.getInstance().setScreen(this.lastScreen);
        }
    }

    protected void handleLocalFiles() {
        BaseEnv.narutoConfig.config.put("videoFileName", this.videoBox.getValue()).put("audioFileName", this.audioBox.getValue()).saveToFile();
        BaseEnv.setupEnv(false);
        NarutoRenderer.INSTANCE.shutdown();
        NarutoRenderer.INSTANCE.setup();
    }

    protected void download(String videoUrl, String audioUrl, String folderName) {
        Path outputDir = YtDlpDownloader.getDefaultOutputDir(folderName);

        NarutoLoading.LOGGER.info("{}Starting URL download with folder name: {}", NarutoLoading.info(), folderName);

        CompletableFuture<Void> videoFuture = YtDlpDownloader.download(BaseEnv.ytDlpProvider.absoluteYtDlp, videoUrl, outputDir, "video", YtDlpDownloader.DownloadType.VIDEO, null, downloadResult -> this.onVideoDownloaded(downloadResult, videoUrl));

        if (videoFuture != null) {
            videoFuture.thenRun(() -> {
                String audioUrlToUse = Strings.isBlank(audioUrl) ? videoUrl : audioUrl;
                if (audioUrlToUse.startsWith("http")) {
                    CompletableFuture<Void> audioFuture = YtDlpDownloader.download(BaseEnv.ytDlpProvider.absoluteYtDlp, audioUrlToUse, outputDir, "audio", YtDlpDownloader.DownloadType.AUDIO, null, downloadResult -> this.onAudioDownloaded(downloadResult, audioUrlToUse));
                    if (audioFuture != null) audioFuture.thenRun(this::finalizeDownload);
                } else {
                    this.finalizeDownload();
                }
            });
        }
    }

    protected void onVideoDownloaded(YtDlpDownloader.@NotNull DownloadResult downloadResult, String videoUrl) {
        NarutoLoading.LOGGER.info("{} Video download of {} processed. {}", NarutoLoading.info(), videoUrl, downloadResult.toString());
        if (downloadResult.success()) Minecraft.getInstance().execute(() -> BaseEnv.narutoConfig.config.put("videoFileName", NarutoConfig.relative(downloadResult.videoPath())).saveToFile());
    }

    protected void onAudioDownloaded(YtDlpDownloader.@NotNull DownloadResult downloadResult, String audioUrlToUse) {
        NarutoLoading.LOGGER.info("{} Audio download of {} processed. {}", NarutoLoading.info(), audioUrlToUse, downloadResult.toString());
        if (downloadResult.success()) {
            Minecraft.getInstance().execute(() -> BaseEnv.narutoConfig.config.put("audioFileName", NarutoConfig.relative(downloadResult.audioPath())).saveToFile());
        }
    }

    protected void finalizeDownload() {
        Minecraft.getInstance().execute(() -> {
            BaseEnv.setupEnv(false);
            NarutoRenderer.INSTANCE.shutdown();
            NarutoRenderer.INSTANCE.setup();
            NarutoLoading.LOGGER.info("{}URL download completed, video playback started", NarutoLoading.info());
        });
    }

    protected void onRandom() {
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
    public void tick() {
        super.tick();
        this.videoBox.tick();
        this.audioBox.tick();
    }

    @Override
    public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        this.font.draw(graphics, VIDEO, centerX, this.videoBox.y - 12, 0xFFFFFF);
        this.font.draw(graphics, AUDIO, centerX, this.audioBox.y - 12, 0xFFFFFF);

        for (GuiEventListener guiEventListener : this.children()) {
            if (guiEventListener instanceof EditBox) ((EditBox)guiEventListener).render(graphics, mouseX, mouseY, partialTick);
            if (guiEventListener instanceof Checkbox) ((Checkbox)guiEventListener).render(graphics, mouseX, mouseY, partialTick);
        }
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

    @Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
    public static final class Trigger {
        public static int interval = 0;

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void clientTick(TickEvent.@NotNull ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;

            Minecraft mc = Minecraft.getInstance();

            if (interval > 0) {
                interval--;
                return;
            }

            long window = mc.getWindow().getWindow();
            int state = GLFW.glfwGetKey(window, BaseEnv.narutoConfig.reload);
            int stateLeftCtrl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL);
            int stateRightCtrl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL);

            if (state == GLFW.GLFW_PRESS && (stateLeftCtrl == GLFW.GLFW_PRESS || stateRightCtrl == GLFW.GLFW_PRESS) && !(mc.screen instanceof SourcesSelectionScreen)) {
                interval = 20;
                mc.setScreen(new SourcesSelectionScreen(mc.screen));
            }
        }
    }
}