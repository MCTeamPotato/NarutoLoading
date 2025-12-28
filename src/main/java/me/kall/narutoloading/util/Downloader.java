package me.kall.narutoloading.util;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Downloader {
    public static void download(@NotNull Path gamePath, @NotNull OSType os, String url) throws Exception {
        Path tmp = gamePath.resolve("ffmpeg-download.tmp");
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }

        Path targetDir = gamePath.resolve("ffmpeg");

        if (os == OSType.WINDOWS) {
            Extractor.unzip(tmp, targetDir);
        } else {
            Extractor.untarXz(tmp, targetDir);
        }

        Files.deleteIfExists(tmp);
    }
}
