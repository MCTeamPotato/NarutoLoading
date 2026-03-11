package me.kall.narutoloading.inworld.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.Strings;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.common.env.ytdlp.YtDlpDownloader;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.data.HiddenDisplayers;
import me.kall.narutoloading.common.env.ffmpeg.AudioConverter;
import me.kall.narutoloading.inworld.data.Screens;
import me.kall.narutoloading.inworld.gui.util.ResourceZipGenerator;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import me.kall.narutoloading.inworld.network.ArgUpdatePacket;
import me.kall.narutoloading.inworld.network.ClearScreenPacket;
import me.kall.narutoloading.inworld.network.SourceSelectionPacket;
import me.kall.narutoloading.noworld.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class InWorldSelectionScreen extends SourcesSelectionScreen {
    private final NarutoInWorldRenderer renderer;
    private Checkbox localSoundCheck;
    private Checkbox hideInnerCheck;

    private EditBox widthBox;
    private EditBox heightBox;

    private EditBox volumeBox;

    public static final Component LOCAL_SOUND = new TranslatableComponent("box.narutoloading.local_sound");
    public static final Component HIDE_INNER = new TranslatableComponent("box.narutoloading.hide_inner");
    public static final Component VIDEO_WIDTH = new TranslatableComponent("box.narutoloading.video_width");
    public static final Component VIDEO_HEIGHT = new TranslatableComponent("box.narutoloading.video_height");
    public static final Component VOLUME = new TranslatableComponent("box.narutoloading.sound_volume");

    public static final Component CLEAR = new TranslatableComponent("button.narutoloading.clear");

    public InWorldSelectionScreen(Screen lastScreen, NarutoInWorldRenderer renderer) {
        super(lastScreen);
        this.renderer = renderer;
    }

    @Override
    public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        this.font.draw(graphics, VIDEO_WIDTH, this.widthBox.x + (float) this.widthBox.getInnerWidth() / 2, this.widthBox.y - 12, 0xFFFFFF);
        this.font.draw(graphics, VIDEO_HEIGHT, this.heightBox.x + (float) this.heightBox.getInnerWidth() / 2, this.heightBox.y - 12, 0xFFFFFF);
        this.font.draw(graphics, VOLUME, (float) this.width / 2, this.volumeBox.y - 12, 0xFFFFFF);
    }

    protected void editBoxes(int centerX, int boxWidth, int boxHeight, int editBoxSpacing) {
        this.widthBox = new EditBox(this.font, centerX - boxWidth / 2, this.currentY, 95, boxHeight, VIDEO_WIDTH);
        this.widthBox.setMaxLength(1024);
        this.widthBox.setValue(String.valueOf(this.renderer.screen.videoWidth()));
        this.widthBox.setFilter(this::validSize);
        this.addWidget(this.widthBox);

        this.heightBox = new EditBox(this.font, centerX + 5, this.currentY, 95, boxHeight, VIDEO_HEIGHT);
        this.heightBox.setMaxLength(1024);
        this.heightBox.setValue(String.valueOf(this.renderer.screen.videoHeight()));
        this.heightBox.setFilter(this::validSize);
        this.addWidget(this.heightBox);

        this.currentY += boxHeight + editBoxSpacing;

        this.volumeBox = new EditBox(this.font, centerX - boxWidth / 2, this.currentY, boxWidth, boxHeight, VOLUME);
        this.volumeBox.setMaxLength(1024);
        this.volumeBox.setValue(String.valueOf(this.renderer.screen.soundVolume()));
        this.volumeBox.setFilter(this::validVolume);
        this.addWidget(this.volumeBox);
    }

    protected void checkBoxes(int centerX, int boxHeight) {
        int checkWidth = 200;
        int checkBoxSpacing = 5;

        this.localSoundCheck = new Checkbox(centerX - checkWidth / 2, this.currentY, checkWidth, boxHeight, LOCAL_SOUND, this.renderer.screen.isLocalSound());
        this.addWidget(this.localSoundCheck);
        this.currentY += boxHeight + checkBoxSpacing;

        this.hideInnerCheck = new Checkbox(centerX - checkWidth / 2, this.currentY, checkWidth, boxHeight, HIDE_INNER, this.renderer.screen.hideInner());
        this.addWidget(this.hideInnerCheck);
        this.currentY += boxHeight + checkBoxSpacing;
    }

    protected void buttons(int centerX, int buttonWidth, int buttonHeight) {
        Button random = new Button(centerX - buttonWidth - 5, this.currentY, buttonWidth, buttonHeight, RANDOM, button -> onRandom());
        this.addButton(random);

        Button clear = new Button(centerX + 5, this.currentY, buttonWidth, buttonHeight, CLEAR, button -> NarutoPackets.INSTANCE.sendToServer(new ClearScreenPacket(this.renderer.screen)));
        this.addButton(clear);
        this.currentY += buttonHeight + 5;
    }

    protected String initVideo() {
        return this.renderer.screen.relativeVideoPath(BaseEnv.narutoConfig.videoFileName);
    }

    protected String initAudio() {
        return this.renderer.screen.relativeAudioPath(BaseEnv.narutoConfig.audioFileName);
    }

    private boolean validSize(@NotNull String value) {
        if (value.isEmpty()) return true;
        try {
            int num = Integer.parseInt(value);
            return num > 0 && num <= 7680;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validVolume(@NotNull String value) {
        if (value.isEmpty()) return true;
        try {
            float vol = Float.parseFloat(value);
            return vol >= 0.0F;
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

    private float volume() {
        try {
            float vol = Float.parseFloat(this.volumeBox.getValue());
            return vol >= 0.0F ? vol : 4.0F;
        } catch (NumberFormatException e) {
            return 4.0F;
        }
    }

    @Override
    protected void handleLocalFiles() {
        this.renderer.screen.setPath(this.videoBox.getValue(), Strings.isBlank(this.audioBox.getValue()) ? this.videoBox.getValue() : this.audioBox.getValue());
        this.renderer.screen.setSize(this.width(), this.height());
        this.renderer.screen.setSoundVolume(this.volume());

        if (this.hideInnerCheck.selected()) {
            if (!this.renderer.screen.hideInner()) {
                this.renderer.screen.setHideInner(true);
                HiddenDisplayers.hide(this.renderer.screen);
            }
        } else {
            if (this.renderer.screen.hideInner()) {
                this.renderer.screen.setHideInner(false);
                HiddenDisplayers.reveal(this.renderer.screen);
            }
        }

        Minecraft.getInstance().levelRenderer.allChanged();

        this.renderer.pause = true;
        this.renderer.shutdown();

        if (this.localSoundCheck.selected()) {
            this.setupLocalSound();
        } else {
            this.renderer.screen.setLocalSound(InWorldScreen.NO_LOCAL_SOUND);
            this.renderer.setup();
            this.renderer.pause = false;
        }

        NarutoPackets.INSTANCE.sendToServer(new ArgUpdatePacket(this.renderer.screen));
    }

    @Override
    protected void onVideoDownloaded(YtDlpDownloader.@NotNull DownloadResult downloadResult, String videoUrl) {
        NarutoLoading.LOGGER.info("{}Video download of {} processed. {}", NarutoLoading.info(), videoUrl, downloadResult.toString());
        if (downloadResult.success() && downloadResult.hasVideo()) {
            Minecraft.getInstance().execute(() -> {
                String relativePath = NarutoConfig.relative(downloadResult.videoPath());
                this.renderer.screen.setPath(relativePath, relativePath);
                NarutoLoading.LOGGER.info("{}Set video path to: {}", NarutoLoading.info(), relativePath);
            });
        }
    }

    @Override
    protected void onAudioDownloaded(YtDlpDownloader.@NotNull DownloadResult downloadResult, String audioUrlToUse) {
        NarutoLoading.LOGGER.info("{}Audio download of {} processed. {}", NarutoLoading.info(), audioUrlToUse, downloadResult.toString());
        if (downloadResult.success() && downloadResult.hasAudio()) {
            Minecraft.getInstance().execute(() -> {
                String relativePath = NarutoConfig.relative(downloadResult.audioPath());
                String currentVideo = this.renderer.screen.relativeVideoPath(NarutoLoading.BLANK);
                this.renderer.screen.setPath(currentVideo, relativePath);
                NarutoLoading.LOGGER.info("{}Set audio path to: {}", NarutoLoading.info(), relativePath);
            });
        }
    }

    @Override
    protected void finalizeDownload() {
        Minecraft.getInstance().execute(() -> {
            this.renderer.screen.setSize(this.width(), this.height());
            this.renderer.screen.setSoundVolume(this.volume());

            if (this.hideInnerCheck.selected()) {
                if (!this.renderer.screen.hideInner()) {
                    this.renderer.screen.setHideInner(true);
                    HiddenDisplayers.hide(this.renderer.screen);
                }
            } else {
                if (this.renderer.screen.hideInner()) {
                    this.renderer.screen.setHideInner(false);
                    HiddenDisplayers.reveal(this.renderer.screen);
                }
            }

            this.renderer.pause = true;
            this.renderer.shutdown();

            if (this.localSoundCheck.selected()) {
                this.setupLocalSound();
            } else {
                this.renderer.screen.setLocalSound(InWorldScreen.NO_LOCAL_SOUND);
                this.renderer.setup();
                this.renderer.pause = false;
            }

            NarutoPackets.INSTANCE.sendToServer(new ArgUpdatePacket(this.renderer.screen));

            NarutoLoading.LOGGER.info("{}URL download and setup completed for in-world screen: {}", NarutoLoading.info(), this.renderer.screen.toString());
        });
    }

    private void setupLocalSound() {
        this.renderer.screen.setLocalSound(InWorldScreen.HAS_LOCAL_SOUND);
        AudioConverter audioConverter = new AudioConverter(NarutoConfig.absolute(this.renderer.screen.relativeAudioPath(NarutoLoading.BLANK)), BaseEnv.ffmpegProvider.absoluteFFmpeg, BaseEnv.ffmpegProvider.absoluteFFprobe);
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
        public static void rightClickDisplayer(PlayerInteractEvent.@NotNull RightClickBlock event) {
            BlockPos pos = event.getPos();
            if (!(event.getWorld() instanceof ServerLevel)) return;
            ServerLevel level = (ServerLevel) event.getWorld();
            if (!(event.getEntity() instanceof ServerPlayer)) return;
            ServerPlayer player = (ServerPlayer) event.getEntity();
            if (!Displayers.isDisplayer(level, pos.asLong())) return;
            if (interval > 0) return;

            NarutoPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SourceSelectionPacket(pos.asLong()));
            interval = 20;
        }

        @SubscribeEvent
        public static void rightClickHangingEntity(PlayerInteractEvent.@NotNull EntityInteract event) {
            if (!(event.getTarget() instanceof HangingEntity)) return;
            HangingEntity hanging = (HangingEntity) event.getTarget();
            if (!(event.getWorld() instanceof ServerLevel)) return;
            ServerLevel level = (ServerLevel) event.getWorld();
            if (!(event.getEntity() instanceof ServerPlayer)) return;
            ServerPlayer player = (ServerPlayer) event.getEntity();
            if (interval > 0) return;

            if (player.isShiftKeyDown() && player.getMainHandItem().getItem().equals(Items.STICK)) return;

            Direction facing = hanging.getDirection();
            long wallPos = hanging.blockPosition().relative(facing.getOpposite()).asLong();

            boolean belongsToScreen = Screens.get(level).screens.values().stream().flatMap(Collection::stream).anyMatch(s -> s.borderInvolved().contains(wallPos));

            if (!belongsToScreen) return;

            event.setCanceled(true);
            NarutoPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SourceSelectionPacket(wallPos));
            interval = 20;
        }
    }
}