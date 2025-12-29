package me.kall.narutoloading.common.env;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.narutoloading.NarutoLoading;
import net.minecraftforge.fml.loading.FMLLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class SourceCollector {
    private static final Path SOURCE_DIRECTORY = FMLLoader.getGamePath().resolve("config").resolve(NarutoLoading.MOD_ID + "-sources");
    private static final String VIDEO_FILE_NAME = "video";
    private static final String AUDIO_FILE_NAME = "audio";

    public static final Set<Source> ABSOLUTE_SOURCES = new ObjectOpenHashSet<>();

    public static void scan() {
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

                if (video != null && audio != null) ABSOLUTE_SOURCES.add(new Source(video.toAbsolutePath().toString(), audio.toAbsolutePath().toString()));
            }
        } catch (Exception e) {
            NarutoLoading.LOGGER.error("Error scanning NarutoLoading sources", e);
        }
    }

    public record Source(String video, String audio) {}
}
