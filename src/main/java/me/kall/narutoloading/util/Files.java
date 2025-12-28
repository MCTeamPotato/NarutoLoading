package me.kall.narutoloading.util;

import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class Files {
    public static String validExe(String path) {
        if (path == null || path.isBlank()) return "";
        File file = new File(path);
        return file.exists() ? file.getAbsolutePath() : "";
    }

    public static @NotNull File getExe(String baseDir, String fileName) {
        return FMLLoader.getGamePath().resolve(baseDir).resolve("bin").resolve(fileName).toFile();
    }
}
