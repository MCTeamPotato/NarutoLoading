package me.kall.narutoloading.data;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class Sources {
    private static final String VIDEO_FILE_NAME = "video";
    private static final String AUDIO_FILE_NAME = "audio";

    public static final List<Source> SOURCES = new ArrayList<>();

    private static final Set<Source> ROLLED = new HashSet<>();

    public static @Nullable Source rollSource() {
        Sources.scanSources();
        if (SOURCES.isEmpty()) return null;
        Source source = SOURCES.get(ThreadLocalRandom.current().nextInt(SOURCES.size()));
        if (SOURCES.size() > 1) {
            while (ROLLED.contains(source)) {
                source = SOURCES.get(ThreadLocalRandom.current().nextInt(SOURCES.size()));
            }
        }

        ROLLED.add(source);
        return source;
    }

    private static void scanSources() {
        SOURCES.clear();

        try (Stream<Path> sources = Files.list(Paths.SOURCES)) {
            List<Path> subDirectories = sources.filter(Files::isDirectory).toList();

            for (Path subDirectory : subDirectories) {
                Path video = null;
                Path audio = null;

                try (Stream<Path> files = Files.list(subDirectory)) {
                    for (Path file : files.filter(Files::isRegularFile).toList()) {
                        String name = file.getFileName().toString();
                        if (name.startsWith(VIDEO_FILE_NAME) && !name.endsWith(".ogg")) {
                            video = file;
                        } else if (name.startsWith(AUDIO_FILE_NAME)) {
                            audio = file;
                        }
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }

                if (video != null) {
                    SOURCES.add(new Source(video.toAbsolutePath().toString(), audio == null ? video.toAbsolutePath().toString() : audio.toAbsolutePath().toString()));
                }
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        if (SOURCES.size() == ROLLED.size()) ROLLED.clear();
    }

    public record Source(String absoluteVideoPath, String absoluteAudioPath) {
        @Override
        public boolean equals(Object object) {
            if (object instanceof Source source) {
                return source.absoluteAudioPath.equals(this.absoluteAudioPath) && source.absoluteVideoPath.equals(this.absoluteVideoPath);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.absoluteAudioPath, this.absoluteAudioPath);
        }
    }
}
