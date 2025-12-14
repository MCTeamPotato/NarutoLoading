package me.kall.narutoloading.core;

import me.kall.narutoloading.NarutoLoading;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class FFmpegDetector {
    private static final Path GAME_DIR = FMLLoader.getGamePath();
    private static final String FFMPEG_NAME = isWindows() ? "ffmpeg.exe" : "ffmpeg";
    private static final String FFPROBE_NAME = isWindows() ? "ffprobe.exe" : "ffprobe";

    private static final String[] SEARCH_SUBDIRS = {"", "bin", "ffmpeg", "tools"};

    public static final String FFMPEG = detectFFmpeg();
    public static final String FFPROBE = detectFFprobe();

    private static String detectFFmpeg() {
        return detectExecutable(FFMPEG_NAME);
    }

    private static String detectFFprobe() {
        return detectExecutable(FFPROBE_NAME);
    }

    private static @Nullable String detectExecutable(String fileName) {
        for (String subDir : SEARCH_SUBDIRS) {
            Path searchPath = GAME_DIR.resolve(subDir);
            try (Stream<Path> walk = Files.walk(searchPath, 3)) {
                return walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().equalsIgnoreCase(fileName))
                        .filter(Files::isExecutable)
                        .map(Path::toAbsolutePath)
                        .map(Path::toString)
                        .findFirst()
                        .orElse(null);
            } catch (IOException e) {
                NarutoLoading.LOGGER.warn("Error while searching for {} in {}: {}", fileName, searchPath, e.getMessage());
            }
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}