package me.kall.narutoloading.data;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
public final class SourceRoller {
    private static final Path SOURCES_ROOT = FMLLoader.getGamePath().resolve("config").resolve("narutoloading-sources").toAbsolutePath();
    private static final String VIDEO_FILE_NAME = "video.mp4";
    private static final String AUDIO_FILE_NAME = "audio.mp3";

    private Path lastSelectedFolder = null;
    private final NarutoRenderer renderer;

    public SourceRoller(NarutoRenderer renderer) {
        this.renderer = renderer;
    }

    public void init() {
        if (!Files.isDirectory(SOURCES_ROOT)) {
            NarutoLoading.LOGGER.info("narutoloading-sources directory not found.");
            return;
        }

        roll();
    }

    private void roll() {
        List<Path> validFolders = sources();

        if (validFolders.isEmpty()) {
            NarutoLoading.LOGGER.warn("No valid source folders found in narutoloading-sources, falling back to default.");
            return;
        }

        if (validFolders.size() == 1) {
            Path selectedFolder = validFolders.get(0);
            selectAndSave(selectedFolder);
            return;
        }

        List<Path> candidates = new ArrayList<>(validFolders);
        if (this.lastSelectedFolder != null) {
            candidates.removeIf(p -> p.getFileName().equals(this.lastSelectedFolder.getFileName()));
        }

        if (candidates.isEmpty()) {
            candidates = validFolders;
        }

        Path selectedFolder = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

        selectAndSave(selectedFolder);
    }

    private void selectAndSave(@NotNull Path selectedFolder) {
        this.lastSelectedFolder = selectedFolder;

        String relativeVideo = "narutoloading-sources/" + selectedFolder.getFileName() + "/" + VIDEO_FILE_NAME;
        String relativeAudio = Files.exists(selectedFolder.resolve(AUDIO_FILE_NAME)) ? "narutoloading-sources/" + selectedFolder.getFileName() + "/" + AUDIO_FILE_NAME : "";

        NarutoLoading.LOGGER.info("SourceRoller selected: folder={}, video={}, audio={}", selectedFolder.getFileName(), relativeVideo, relativeAudio.isEmpty() ? "(video embedded)" : relativeAudio);

        this.renderer.narutoConfig.config.put("videoFileName", relativeVideo).put("audioFileName", relativeAudio).saveToFile();

        this.renderer.narutoConfig.init();
    }

    private static @NotNull List<Path> sources() {
        List<Path> valid = new ArrayList<>();

        try (Stream<Path> stream = Files.list(SOURCES_ROOT)) {
            stream.filter(Files::isDirectory).forEach(folder -> {
                Path video = folder.resolve(VIDEO_FILE_NAME);
                if (Files.isRegularFile(video)) valid.add(folder);
            });
        } catch (IOException e) {
            NarutoLoading.LOGGER.error("Failed to scan narutoloading-sources directory", e);
        }

        return valid;
    }

    public static int sourceRollable = 0;

    @SubscribeEvent
    public static void clientTick(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getInstance();

        if (sourceRollable > 0) {
            sourceRollable--;
            return;
        }

        long window = mc.getWindow().getWindow();
        int state = GLFW.glfwGetKey(window, NarutoRenderer.INSTANCE.narutoConfig.reload);
        int stateShift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT);

        if (state == GLFW.GLFW_PRESS && stateShift == GLFW.GLFW_PRESS) {
            sourceRollable = 20;
            NarutoRenderer.INSTANCE.shutdown();
            NarutoRenderer.INSTANCE.setup();
        }
    }
}