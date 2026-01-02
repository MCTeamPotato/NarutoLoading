package me.kall.narutoloading.common.env;

import me.kall.narutoloading.NarutoLoading;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class FFmpegProvider {
    public volatile @Nullable String absoluteFFmpeg;
    public volatile @Nullable String absoluteFFprobe;

    private final String uncheckedAbsoluteFFprobePath, uncheckedAbsoluteFFmpegPath, winUrl, linuxUrl;

    private final ExecutorService downloader;

    public FFmpegProvider(String uncheckedAbsoluteFFprobePath, String uncheckedAbsoluteFFmpegPath, String winUrl, String linuxUrl) {
        this.uncheckedAbsoluteFFprobePath = uncheckedAbsoluteFFprobePath;
        this.uncheckedAbsoluteFFmpegPath = uncheckedAbsoluteFFmpegPath;
        this.winUrl = winUrl;
        this.linuxUrl = linuxUrl;
        this.downloader = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task , "NarutoFFmpegDownloader");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void setup(Runnable onDone) {
        String absoluteFFprobePath = Executable.validExe(this.uncheckedAbsoluteFFprobePath);
        String absoluteFFmpegPath = Executable.validExe(this.uncheckedAbsoluteFFmpegPath);

        if (!absoluteFFprobePath.isBlank() && !absoluteFFmpegPath.isBlank()) {
            this.absoluteFFprobe = absoluteFFprobePath;
            this.absoluteFFmpeg = absoluteFFmpegPath;
            NarutoLoading.LOGGER.info("{}Using FFmpeg from config.", NarutoLoading.info());
            onDone.run();
            return;
        }

        OSType os = OSType.CURRENT;
        Path gamePath = FMLLoader.getGamePath();

        boolean windows = os == OSType.WINDOWS;
        String ffmpegName = windows ? "ffmpeg.exe" : "ffmpeg";
        String ffprobeName = windows ? "ffprobe.exe" : "ffprobe";

        this.downloader.submit(() -> {
            String baseDir = OSType.getBase(gamePath, os);

            if (baseDir == null && os != null) {
                try {
                    NarutoLoading.LOGGER.info("{}Downloading FFmpeg...", NarutoLoading.info());
                    Downloader.download(gamePath, os, os.equals(OSType.WINDOWS) ? this.winUrl : this.linuxUrl);
                    baseDir = OSType.getBase(gamePath, os);
                } catch (Exception exception) {
                    NarutoLoading.LOGGER.error("Error downloading FFmpeg.", exception);
                } finally {
                    NarutoLoading.LOGGER.info("{}FFmpeg download task ends.", NarutoLoading.info());
                }
            }

            if (baseDir == null) {
                this.absoluteFFmpeg = null;
                this.absoluteFFprobe = null;
                onDone.run();
                return;
            }

            File ffmpegFile = Executable.getExe(baseDir, ffmpegName);
            File ffprobeFile = Executable.getExe(baseDir, ffprobeName);

            this.absoluteFFmpeg = ffmpegFile.exists() ? ffmpegFile.getAbsolutePath() : null;
            this.absoluteFFprobe = ffprobeFile.exists() ? ffprobeFile.getAbsolutePath() : null;
            NarutoLoading.LOGGER.info("{}NarutoLoading ffmpeg file path: {}", NarutoLoading.info(), this.absoluteFFmpeg);
            NarutoLoading.LOGGER.info("{}NarutoLoading ffprobe file path: {}", NarutoLoading.info(), this.absoluteFFprobe);
            onDone.run();
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

        static @NotNull File getExe(String baseDir, String fileName) {
            return FMLLoader.getGamePath().resolve(baseDir).resolve("bin").resolve(fileName).toFile();
        }
    }

    static class Downloader {
        static void download(@NotNull Path gamePath, @NotNull OSType os, String url) throws Exception {
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

    static class Extractor {
        static void untarXz(@NotNull Path archive, Path targetDir) throws Exception {
            Files.createDirectories(targetDir);

            Process process = new ProcessBuilder("tar", "-xJf", archive.toAbsolutePath().toString(), "-C", targetDir.toAbsolutePath().toString(), "--strip-components=1").inheritIO().start();

            int code = process.waitFor();
            if (code != 0) {
                throw new RuntimeException("tar failed with exit code " + code);
            }
        }

        static void unzip(Path zip, Path targetDir) throws Exception {
            try (ZipInputStream zis = new ZipInputStream(java.nio.file.Files.newInputStream(zip))) {
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

    enum OSType {
        WINDOWS("ffmpeg-win"),
        LINUX("ffmpeg-linux");

        static final OSType CURRENT = current();

        final String dirName;

        OSType(String dirName) {
            this.dirName = dirName;
        }

        static @Nullable FFmpegProvider.OSType current() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) return WINDOWS;
            if (os.contains("linux")) return LINUX;
            return null;
        }

        static @Nullable String getBase(Path gamePath, @Nullable FFmpegProvider.OSType os) {
            if (os != null) {
                Path osDir = gamePath.resolve(os.dirName);
                if (osDir.toFile().exists()) {
                    return os.dirName;
                }
            }

            return gamePath.resolve("ffmpeg").toFile().exists() ? "ffmpeg" : null;
        }
    }
}
