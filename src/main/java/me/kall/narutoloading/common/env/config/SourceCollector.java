package me.kall.narutoloading.common.env.config;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class SourceCollector {
    private static final Path SOURCE_DIRECTORY = FMLLoader.getGamePath().resolve("config").resolve(NarutoLoading.MOD_ID + "-sources");
    private static final String VIDEO_FILE_NAME = "video";
    private static final String AUDIO_FILE_NAME = "audio";

    public static final List<Source> ABSOLUTE_SOURCES = new ObjectArrayList<>();

    private static Source lastSource;

    public static @Nullable Source roll() {
        scan();
        if (ABSOLUTE_SOURCES.isEmpty()) return null;
        NarutoLoading.LOGGER.info("Start to roll source from {}", ABSOLUTE_SOURCES.stream().map(source -> "{Video: " + source.absoluteVideoPath + ". Audio: " + source.absoluteAudioPath + "}").toList());
        Source source = ABSOLUTE_SOURCES.get(ThreadLocalRandom.current().nextInt(SourceCollector.ABSOLUTE_SOURCES.size()));
        if (ABSOLUTE_SOURCES.size() > 1) {
            while (source.equals(lastSource)) {
                source = ABSOLUTE_SOURCES.get(ThreadLocalRandom.current().nextInt(SourceCollector.ABSOLUTE_SOURCES.size()));
            }
        }
        NarutoLoading.LOGGER.info("Rolling source ends. Video: {}, Audio: {}", source.absoluteVideoPath, source.absoluteAudioPath);
        lastSource = source;
        return source;
    }

    public static void scan() {
        ABSOLUTE_SOURCES.clear();
        try (Stream<Path> stream = Files.list(SOURCE_DIRECTORY)) {
            List<Path> subDirs = stream.filter(Files::isDirectory).toList();

            for (Path subDir : subDirs) {
                Path video = null;
                Path audio = null;

                try (Stream<Path> files = Files.list(subDir)) {
                    for (Path file : files.filter(Files::isRegularFile).toList()) {
                        String name = file.getFileName().toString();

                        if (name.startsWith(VIDEO_FILE_NAME) && !name.endsWith(".ogg")) {
                            video = file;
                        } else if (name.startsWith(AUDIO_FILE_NAME)) {
                            audio = file;
                        }
                    }
                }

                if (video != null) {
                    ABSOLUTE_SOURCES.add(new Source(video.toAbsolutePath().toString(), audio == null ? video.toAbsolutePath().toString() : audio.toAbsolutePath().toString()));
                }
            }
        } catch (Exception e) {
            NarutoLoading.LOGGER.error("Error scanning NarutoLoading sources", e);
        }
    }

    @Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID, value = Dist.CLIENT)
    public static final class ManualTrigger {
        public static int interval = 0;

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void clientTick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getInstance();

            if (interval > 0) {
                interval--;
                return;
            }

            long window = mc.getWindow().getWindow();
            int state = GLFW.glfwGetKey(window, BaseEnv.narutoConfig.reload);
            int stateLeftShift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT);
            int stateRightShift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

            if (state == GLFW.GLFW_PRESS && (stateLeftShift == GLFW.GLFW_PRESS || stateRightShift == GLFW.GLFW_PRESS)) {
                BaseEnv.setupEnv(true);
                interval = 20;
            }
        }
    }

    public record Source(String absoluteVideoPath, String absoluteAudioPath) {
        @Override
        public boolean equals(Object object) {
            if (object instanceof Source source) {
                return source.absoluteAudioPath.equals(this.absoluteAudioPath) && source.absoluteVideoPath.equals(this.absoluteVideoPath);
            }
            return false;
        }
    }
}
