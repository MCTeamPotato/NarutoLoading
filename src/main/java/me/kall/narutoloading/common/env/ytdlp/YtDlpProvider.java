package me.kall.narutoloading.common.env.ytdlp;

import me.kall.narutoloading.NarutoLoading;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public final class YtDlpProvider {
    public volatile String absoluteYtDlp;

    private final String uncheckedAbsoluteYtDlpPath;

    public YtDlpProvider(String uncheckedAbsoluteYtDlpPath) {
        this.uncheckedAbsoluteYtDlpPath = uncheckedAbsoluteYtDlpPath;
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
        if (os == null) {
            NarutoLoading.LOGGER.error("{}Unsupported operating system for yt-dlp", NarutoLoading.info());
            this.absoluteYtDlp = null;
            onDone.run();
            return;
        }

        Path gamePath = FMLLoader.getCurrent().getGameDir();
        String ytDlpName = os == OSType.WINDOWS ? "yt-dlp.exe" : "yt-dlp";
        File ytDlpFile = gamePath.resolve("yt-dlp").resolve(ytDlpName).toFile();

        if (ytDlpFile.exists()) {
            this.absoluteYtDlp = ytDlpFile.getAbsolutePath();
            NarutoLoading.LOGGER.info("{}NarutoLoading yt-dlp file path: {}", NarutoLoading.info(), this.absoluteYtDlp);
        } else {
            NarutoLoading.LOGGER.warn("{}yt-dlp not found. Please set ytdlpExePath in the config.", NarutoLoading.info());
            this.absoluteYtDlp = null;
        }

        onDone.run();
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