package me.kall.narutoloading.client.ui;

import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Opens a local folder in the system file browser, creating it first when missing.
 */
final class Folders {
    private Folders() {}

    static void open(@NotNull Path folder) {
        try {
            Files.createDirectories(folder);
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Exception creating " + folder + ": " + exception.getMessage());
        }

        try {
            Util.getPlatform().openFile(folder.toFile());
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading] Exception opening " + folder + ": " + throwable.getMessage());
        }
    }
}
