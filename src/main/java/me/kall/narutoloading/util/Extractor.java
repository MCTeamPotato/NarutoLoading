package me.kall.narutoloading.util;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Extractor {
    public static void untarXz(@NotNull Path archive, Path targetDir) throws Exception {
        Files.createDirectories(targetDir);

        Process process = new ProcessBuilder("tar", "-xJf", archive.toAbsolutePath().toString(), "-C", targetDir.toAbsolutePath().toString(), "--strip-components=1").inheritIO().start();

        int code = process.waitFor();
        if (code != 0) {
            throw new RuntimeException("tar failed with exit code " + code);
        }
    }

    public static void unzip(Path zip, Path targetDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                int firstSlash = name.indexOf('/');
                if (firstSlash < 0) continue;

                Path out = targetDir.resolve(name.substring(firstSlash + 1));
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
