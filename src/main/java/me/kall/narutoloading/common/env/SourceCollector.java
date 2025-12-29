package me.kall.narutoloading.common.env;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.kall.narutoloading.NarutoLoading;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.Nullable;

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
        if (ABSOLUTE_SOURCES.isEmpty()) return null;
        Source source = ABSOLUTE_SOURCES.get(ThreadLocalRandom.current().nextInt(SourceCollector.ABSOLUTE_SOURCES.size()));
        if (ABSOLUTE_SOURCES.size() > 1) {
            while (source == lastSource) {
                source = ABSOLUTE_SOURCES.get(ThreadLocalRandom.current().nextInt(SourceCollector.ABSOLUTE_SOURCES.size()));
            }
        }
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

                        if (name.startsWith(VIDEO_FILE_NAME)) {
                            video = file;
                        } else if (name.startsWith(AUDIO_FILE_NAME)) {
                            audio = file;
                        }
                    }
                }

                if (video != null && audio != null) {
                    ABSOLUTE_SOURCES.add(new Source(video.toAbsolutePath().toString(), audio.toAbsolutePath().toString()));
                }
            }
        } catch (Exception e) {
            NarutoLoading.LOGGER.error("Error scanning NarutoLoading sources", e);
        }
    }

    public record Source(String absoluteVideoPath, String absoluteAudioPath) {}
}
