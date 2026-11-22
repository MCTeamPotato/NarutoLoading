package me.kall.narutoloading.core;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class GamePaths {
    private static final String GAME_DIR = "user.dir";

    private GamePaths() {}

    public static @NotNull Path gameDir() {
        return Path.of(System.getProperty(GAME_DIR, "."));
    }

    public static @NotNull Path resolve(@NotNull Path path) {
        return path.isAbsolute() ? path : gameDir().resolve(path);
    }
}
