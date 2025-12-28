package me.kall.narutoloading.util;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public enum OSType {
    WINDOWS("ffmpeg-win"),
    LINUX("ffmpeg-linux");

    public static final OSType CURRENT = current();

    public final String dirName;

    OSType(String dirName) {
        this.dirName = dirName;
    }

    static @Nullable OSType current() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return WINDOWS;
        if (os.contains("linux")) return LINUX;
        return null;
    }

    public static @Nullable String getBase(Path gamePath, @Nullable OSType os) {
        if (os != null) {
            Path osDir = gamePath.resolve(os.dirName);
            if (osDir.toFile().exists()) {
                return os.dirName;
            }
        }

        return gamePath.resolve("ffmpeg").toFile().exists() ? "ffmpeg" : null;
    }
}
