package me.kall.narutoloading.common.env.ytdlp;

import me.kall.narutoloading.NarutoLoading;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class YtDlpProvider {
    public volatile String absoluteYtDlp;

    private final String uncheckedAbsoluteYtDlpPath;
    private final String winUrl;
    private final String linuxUrl;
    private final String macUrl;

    private final ExecutorService downloader;

    public YtDlpProvider(String uncheckedAbsoluteYtDlpPath, String winUrl, String linuxUrl, String macUrl) {
        this.uncheckedAbsoluteYtDlpPath = uncheckedAbsoluteYtDlpPath;
        this.winUrl = winUrl;
        this.linuxUrl = linuxUrl;
        this.macUrl = macUrl;
        this.downloader = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoYtDlpDownloader");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void setup(Runnable onDone) {
        String absoluteYtDlpPath = Executable.validExe(this.uncheckedAbsoluteYtDlpPath);

        if (!absoluteYtDlpPath.isBlank()) {
            this.absoluteYtDlp = absoluteYtDlpPath;
            NarutoLoading.LOGGER.info("{}Using yt-dlp from config.", NarutoLoading.info());
            onDone.run();
            return;
        }

        OSType os = OSType.CURRENT;
        Path gamePath = FMLLoader.getCurrent().getGameDir();

        if (os == null) {
            NarutoLoading.LOGGER.error("{}Unsupported operating system for yt-dlp", NarutoLoading.info());
            this.absoluteYtDlp = null;
            onDone.run();
            return;
        }

        boolean windows = os == OSType.WINDOWS;
        String ytDlpName = windows ? "yt-dlp.exe" : "yt-dlp";

        this.downloader.submit(() -> {
            try {
                Path ytdlpDir = gamePath.resolve("yt-dlp");
                Files.createDirectories(ytdlpDir);

                File ytDlpFile = ytdlpDir.resolve(ytDlpName).toFile();

                if (!ytDlpFile.exists()) {
                    NarutoLoading.LOGGER.info("{}Downloading yt-dlp for {}...", NarutoLoading.info(), os);

                    String downloadUrl = switch (os) {
                        case WINDOWS -> this.winUrl;
                        case LINUX -> this.linuxUrl;
                        case MACOS -> this.macUrl;
                    };

                    try (InputStream in = URI.create(downloadUrl).toURL().openStream()) {
                        Files.copy(in, ytDlpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    if (!windows) {
                        if (!ytDlpFile.setExecutable(true, false)) {
                            NarutoLoading.LOGGER.warn("{}Failed to set executable permission for yt-dlp", NarutoLoading.info());
                        }
                    }

                    NarutoLoading.LOGGER.info("{}yt-dlp downloaded successfully to: {}", NarutoLoading.info(), ytDlpFile.getAbsolutePath());
                } else {
                    NarutoLoading.LOGGER.info("{}yt-dlp already exists at: {}", NarutoLoading.info(), ytDlpFile.getAbsolutePath());
                }

                this.absoluteYtDlp = ytDlpFile.exists() ? ytDlpFile.getAbsolutePath() : null;
                NarutoLoading.LOGGER.info("{}NarutoLoading yt-dlp file path: {}", NarutoLoading.info(), this.absoluteYtDlp);

            } catch (Exception exception) {
                NarutoLoading.LOGGER.error("Error downloading yt-dlp.", exception);
                this.absoluteYtDlp = null;
            } finally {
                NarutoLoading.LOGGER.info("{}yt-dlp download task ends.", NarutoLoading.info());
                onDone.run();
            }
        });
    }

    public void shutdown() {
        this.downloader.shutdownNow();
    }

    static class Executable {
        static @NotNull String validExe(String path) {
            if (path == null || path.isBlank()) return NarutoLoading.BLANK;
            File file = new File(path);
            return file.exists() ? file.getAbsolutePath() : NarutoLoading.BLANK;
        }
    }

    enum OSType {
        WINDOWS,
        LINUX,
        MACOS;

        static final OSType CURRENT = current();

        static @Nullable OSType current() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) return WINDOWS;
            if (os.contains("mac")) return MACOS;
            if (os.contains("linux")) return LINUX;
            return null;
        }
    }
}