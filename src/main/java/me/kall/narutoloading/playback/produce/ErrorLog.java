package me.kall.narutoloading.playback.produce;

import me.kall.narutoloading.core.GamePaths;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class ErrorLog {
    private static final File FILE = GamePaths.gameDir().resolve("logs").resolve("narutoloading-error.log").toFile();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH.mm.ss");

    private static boolean initialized;

    private ErrorLog() {}

    public static synchronized @Nullable File initialize() {
        if (initialized) return FILE;
        initialized = true;

        File logDir = FILE.getParentFile();

        if (!logDir.exists()) logDir.mkdirs();

        if (FILE.exists() && FILE.length() > 0) {
            String timeName = LocalDateTime.now().format(TIME_FORMATTER);
            File gzFile = new File(logDir, "narutoloading-" + timeName + ".log.gz");
            try (FileInputStream fileInputStream = new FileInputStream(FILE); FileOutputStream fileOutputStream = new FileOutputStream(gzFile); GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fileInputStream.read(buffer)) != -1) {
                    gzipOutputStream.write(buffer, 0, len);
                }
            } catch (IOException e) {
                System.err.println("[NarutoLoading] Failed to compress old error log: " + e.getMessage());
                return null;
            }

            try {
                Files.delete(FILE.toPath());
            } catch (IOException e) {
                System.err.println("[NarutoLoading] Failed to delete old error log: " + e.getMessage());
                return null;
            }
        }

        try {
            FILE.createNewFile();
        } catch (IOException e) {
            System.err.println("[NarutoLoading] Failed to create new error log: " + e.getMessage());
            return null;
        }

        return FILE;
    }
}
