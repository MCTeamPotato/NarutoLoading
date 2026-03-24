package me.kall.narutoloading.common.util;

import me.kall.narutoloading.NarutoLoading;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class Paths {
    public static final Path CONFIG_DIR = Path.of("D:/HMCL/.minecraft/versions/1.20.1-Forge/config");
    public static final Path GAME_DIR = Path.of("D:/HMCL/.minecraft/versions/1.20.1-Forge");

    public static @NotNull String absolute(@NotNull String relativePath) {
        if (relativePath.isBlank()) return NarutoLoading.BLANK;
        return CONFIG_DIR.resolve(relativePath).toAbsolutePath().toString();
    }

    public static @NotNull String relative(@NotNull String absolutePath) {
        if (absolutePath.isBlank()) return NarutoLoading.BLANK;
        Path configPath = CONFIG_DIR.toAbsolutePath().normalize();
        Path targetPath = Path.of(absolutePath).toAbsolutePath().normalize();
        if (!targetPath.startsWith(configPath)) return NarutoLoading.BLANK;
        return configPath.relativize(targetPath).toString();
    }
}
