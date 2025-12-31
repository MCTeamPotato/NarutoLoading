package me.kall.narutoloading.inworld.gui;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import me.kall.narutoloading.inworld.network.ArgUpdatePacket;
import me.kall.narutoloading.inworld.network.SourceSelectionPacket;
import me.kall.narutoloading.noworld.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class InWorldSelectionScreen extends SourcesSelectionScreen {
    private final NarutoInWorldRenderer renderer;
    private Checkbox cullableCheck;
    private Checkbox localSoundCheck;

    public static final Component CULLABLE = Component.translatable("box.narutoloading.cullable");
    public static final Component LOCAL_SOUND = Component.translatable("box.narutoloading.local_sound");

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
        this.videoBox.setValue(NarutoConfig.relative(this.renderer.screen.absoluteVideoPath(BaseEnv.narutoConfig.absoluteVideoPath)));
        this.addRenderableWidget(this.videoBox);

        this.audioBox = new EditBox(this.font, centerX - boxWidth / 2, centerY - spacing, boxWidth, boxHeight, AUDIO);
        this.audioBox.setMaxLength(1024);
        this.audioBox.setValue(NarutoConfig.relative(this.renderer.screen.absoluteAudioPath(BaseEnv.narutoConfig.absoluteAudioPath)));
        this.addRenderableWidget(this.audioBox);

        int checkY = centerY + spacing / 2;
        int checkWidth = 98;
        int checkGap = 4;

        this.cullableCheck = new Checkbox(centerX - checkWidth - checkGap / 2, checkY, checkWidth, boxHeight, CULLABLE, this.renderer.screen.isCullable());
        this.addRenderableWidget(this.cullableCheck);

        this.localSoundCheck = new Checkbox(centerX + checkGap / 2, checkY, checkWidth, boxHeight, LOCAL_SOUND, this.renderer.screen.isLocalSound());
        this.addRenderableWidget(this.localSoundCheck);

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
        InWorldScreen inWorldScreen = this.renderer.screen;
        inWorldScreen.set(videoFilename, audioFileName.isBlank() ? videoFilename : audioFileName);
        inWorldScreen.setCullable(this.cullableCheck.selected());

        this.renderer.shutdown();

        if (this.localSoundCheck.selected()) {
            this.renderer.screen.setLocalSound(InWorldScreen.HAS_LOCAL_SOUND);
            AudioConverter audioConverter = new AudioConverter(inWorldScreen.absoluteAudioPath(""), BaseEnv.ffmpegProvider.absoluteFFmpeg);
            audioConverter.setup(() -> {
                ResourceZipGenerator resourceZipGenerator = new ResourceZipGenerator(audioConverter.converted);
                resourceZipGenerator.generate();
                resourceZipGenerator.reload(this.renderer);
            });
        } else {
            this.renderer.screen.setLocalSound(InWorldScreen.NO_LOCAL_SOUND);
        }

        this.renderer.setup();

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

    public static final class ResourceZipGenerator {
        private final String convertedAudioPath;
        private final String packName;
        public final String id;

        private static final String RESOURCE_PACKS = FMLLoader.getGamePath().resolve("resourcepacks").toAbsolutePath().toString();

        public ResourceZipGenerator(String convertedAudioPath) {
            this.convertedAudioPath = convertedAudioPath;
            this.id = Paths.get(this.convertedAudioPath).getParent().getFileName().toString();
            this.packName = "NarutoLoadingAudioSource-" + this.id + ".zip";
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        public void generate() {
            try {
                File resourcePacksDir = new File(RESOURCE_PACKS);
                if (!resourcePacksDir.exists()) resourcePacksDir.mkdirs();

                File zipFile = new File(resourcePacksDir, packName);
                if (zipFile.exists()) zipFile.delete();

                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                    addPackMcmeta(zos);
                    addAudioFiles(zos);
                    NarutoLoading.LOGGER.info("Successfully created resource pack: {}", zipFile.getAbsolutePath());
                }

            } catch (Exception e) {
                NarutoLoading.LOGGER.error("Failed to generate resource pack", e);
            }
        }

        public void reload(NarutoInWorldRenderer renderer) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                try {
                    LocalPlayer player = minecraft.player;
                    if (player != null) player.displayClientMessage(Component.translatable("info.narutoloading.local_sound.start", this.packName), false);

                    PackRepository repository = minecraft.getResourcePackRepository();
                    repository.reload();
                    String packId = "file/" + this.packName;

                    Pack pack = repository.getPack(packId);
                    if (pack == null) {
                        NarutoLoading.LOGGER.warn("Could not find pack: {}", packId);
                        return;
                    }

                    Collection<String> selected = new ArrayList<>(repository.getSelectedIds());
                    selected.removeIf(s -> s.equals("file/NarutoLoadingAudioSource-" + this.id + ".zip"));
                    if (!selected.contains(packId)) selected.add(packId);

                    repository.setSelected(selected);

                    minecraft.options.resourcePacks = new ArrayList<>(selected);
                    minecraft.options.save();

                    minecraft.reloadResourcePacks().thenRun(() -> {
                        Minecraft mc = Minecraft.getInstance();
                        mc.execute(() -> {
                            NarutoLoading.LOGGER.info("Successfully activated resource pack: {}", packId);
                            LocalPlayer p = mc.player;
                            if (p != null) {
                                p.displayClientMessage(Component.translatable("info.narutoloading.local_sound.end"), false);

                                renderer.screen.setLocalSound(ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, this.id));

                                NarutoPackets.INSTANCE.sendToServer(new ArgUpdatePacket(renderer.screen));

                                boolean wasRunning = renderer.isRunning();
                                if (wasRunning) renderer.shutdown();
                                renderer.setup();
                            }
                        });
                    });

                } catch (Exception e) {
                    NarutoLoading.LOGGER.error("Error activating resource pack", e);
                }
            });
        }

        private void addPackMcmeta(@NotNull ZipOutputStream zos) throws Exception {
            ZipEntry entry = new ZipEntry("pack.mcmeta");
            zos.putNextEntry(entry);

            String mcmeta = """
                        {
                          "pack": {
                            "pack_format": 9,
                            "description": "NarutoLoading Audio Sources"
                          }
                        }""";

            zos.write(mcmeta.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        private void addAudioFiles(ZipOutputStream zos) throws Exception {
            File audioFile = new File(this.convertedAudioPath);
            if (!audioFile.exists() || !audioFile.isFile()) return;

            String entryPath = String.format("assets/%s/sounds/%s.ogg", NarutoLoading.MOD_ID, this.id);
            ZipEntry entry = new ZipEntry(entryPath);
            zos.putNextEntry(entry);

            try (FileInputStream fis = new FileInputStream(audioFile)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
            }

            zos.closeEntry();
            NarutoLoading.LOGGER.debug("Added audio file to resource pack: {}", entryPath);

            addSoundsJson(zos);
        }

        private void addSoundsJson(@NotNull ZipOutputStream zos) throws Exception {
            ZipEntry entry = new ZipEntry(String.format("assets/%s/sounds.json", NarutoLoading.MOD_ID));
            zos.putNextEntry(entry);

            String json = String.format("""
                {
                  "%s": {
                    "sounds": [
                      "%s:%s"
                    ]
                  }
                }""", this.id, NarutoLoading.MOD_ID, this.id);

            zos.write(json.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    public static class AudioConverter {
        private final String absoluteSourcePath;
        private final String absoluteFFmpegPath;
        private final ExecutorService converter = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task , "NarutoAudioConverter");
            thread.setDaemon(true);
            return thread;
        });
        public String converted = "";

        public AudioConverter(String absoluteSourcePath, String absoluteFFmpegPath) {
            this.absoluteSourcePath = absoluteSourcePath;
            this.absoluteFFmpegPath = absoluteFFmpegPath;
        }

        public void setup(Runnable onDone) {
            File sourceFile = new File(absoluteSourcePath);
            if (!sourceFile.exists() || !sourceFile.isFile()) return;

            String fileName = sourceFile.getName();
            String lowerName = fileName.toLowerCase();

            if (lowerName.endsWith(".ogg")) {
                this.converted = this.absoluteSourcePath;
                onDone.run();
                return;
            }

            Path parentDir = sourceFile.toPath().getParent();
            File absoluteOutputPath = parentDir.resolve(fileName.substring(0, fileName.lastIndexOf(".")) + ".ogg").toFile();

            if (absoluteOutputPath.exists()) {
                this.converted = absoluteOutputPath.getAbsolutePath();
                onDone.run();
                return;
            }

            this.converter.submit(() -> {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder(this.absoluteFFmpegPath, "-i", absoluteSourcePath, "-vn", "-acodec", "libvorbis", "-q:a", "4", "-y", absoluteOutputPath.getAbsolutePath()).redirectErrorStream(true);
                    Process process = processBuilder.start();

                    StringBuilder output = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                        }
                    }

                    int exitCode = process.waitFor();

                    if (exitCode == 0 && absoluteOutputPath.exists()) {
                        NarutoLoading.LOGGER.info("Successfully converted to OGG: {}", absoluteOutputPath.getAbsolutePath());
                        this.converted = absoluteOutputPath.getAbsolutePath();
                    } else {
                        NarutoLoading.LOGGER.error("FFmpeg conversion failed with exit code: {}", exitCode);
                        NarutoLoading.LOGGER.error("FFmpeg output:\n{}", output.toString());
                    }
                } catch (Exception exception) {
                    NarutoLoading.LOGGER.error("Error converting audio ", exception);
                } finally {
                    onDone.run();
                    this.converter.shutdown();
                }
            });
        }
    }
}