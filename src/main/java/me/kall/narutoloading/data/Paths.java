package me.kall.narutoloading.data;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class Paths {
    public static final Path GAME_DIR = Path.of("D:/HMCL/.minecraft/versions/1.20.1-Forge").toAbsolutePath();
    public static final Path CONFIG_DIR = GAME_DIR.resolve("config");
    public static final Path SOURCES = CONFIG_DIR.resolve("narutoloading-sources");

    public static final Path FFMPEG = GAME_DIR.resolve("ffmpeg-win").resolve("bin").resolve("ffmpeg.exe");
    public static final Path FFPROBE = GAME_DIR.resolve("ffmpeg-win").resolve("bin").resolve("ffprobe.exe");

    public static final Path YT_DLP = GAME_DIR.resolve("yt-dlp").resolve("yt-dlp.exe");

    public static @NotNull String absolute(@NotNull String relativePath) {
        if (relativePath.isBlank()) return "";
        return CONFIG_DIR.resolve(relativePath).toAbsolutePath().toString();
    }

    public static @NotNull String relative(@NotNull String absolutePath) {
        if (absolutePath.isBlank()) return "";
        Path configPath = CONFIG_DIR.normalize();
        Path targetPath = Path.of(absolutePath).toAbsolutePath().normalize();
        if (!targetPath.startsWith(configPath)) return "";
        return configPath.relativize(targetPath).toString();
    }
}
