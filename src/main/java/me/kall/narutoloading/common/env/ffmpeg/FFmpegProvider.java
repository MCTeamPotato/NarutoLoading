package me.kall.narutoloading.common.env.ffmpeg;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.Strings;
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

    private final String uncheckedAbsoluteFFprobePath, uncheckedAbsoluteFFmpegPath, winUrl, linuxUrl, macUrl;

    private final ExecutorService downloader;

    public FFmpegProvider(String uncheckedAbsoluteFFprobePath, String uncheckedAbsoluteFFmpegPath, String winUrl, String linuxUrl, String macUrl) {
        this.uncheckedAbsoluteFFprobePath = uncheckedAbsoluteFFprobePath;
        this.uncheckedAbsoluteFFmpegPath = uncheckedAbsoluteFFmpegPath;
        this.winUrl = winUrl;
        this.linuxUrl = linuxUrl;
        this.macUrl = macUrl;
        this.downloader = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task , "NarutoFFmpegDownloader");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void setup(Runnable onDone) {
        String absoluteFFprobePath = Executable.validExe(this.uncheckedAbsoluteFFprobePath);
        String absoluteFFmpegPath = Executable.validExe(this.uncheckedAbsoluteFFmpegPath);

        if (!Strings.isBlank(absoluteFFprobePath) && !Strings.isBlank(absoluteFFmpegPath)) {
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
                    NarutoLoading.LOGGER.info("{}Downloading FFmpeg for {}...", NarutoLoading.info(), os);

                    if (os == OSType.MACOS) {
                        Downloader.downloadMac(gamePath, this.macUrl);
                    } else {
                        Downloader.download(gamePath, os, os.equals(OSType.WINDOWS) ? this.winUrl : this.linuxUrl);
                    }

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

            File ffmpegFile = Executable.getExe(baseDir, ffmpegName, os);
            File ffprobeFile = Executable.getExe(baseDir, ffprobeName, os);

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
            if (path == null || Strings.isBlank(path)) return NarutoLoading.BLANK;
            File file = new File(path);
            return file.exists() ? file.getAbsolutePath() : NarutoLoading.BLANK;
        }

        static @NotNull File getExe(String baseDir, String fileName, OSType os) {
            if (os == OSType.MACOS) {
                return FMLLoader.getGamePath().resolve(baseDir).resolve(fileName).toFile();
            }
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

        static void downloadMac(@NotNull Path gamePath, String baseUrl) throws Exception {
            Path targetDir = gamePath.resolve("ffmpeg-mac");
            Files.createDirectories(targetDir);

            String ffmpegUrl = baseUrl + "/getrelease/ffmpeg/zip";
            Path ffmpegZip = gamePath.resolve("ffmpeg-mac-temp.zip");
            NarutoLoading.LOGGER.info("{}Downloading ffmpeg from: {}", NarutoLoading.info(), ffmpegUrl);

            try (InputStream in = new URL(ffmpegUrl).openStream()) {
                Files.copy(in, ffmpegZip, StandardCopyOption.REPLACE_EXISTING);
            }
            Extractor.unzipSingle(ffmpegZip, targetDir, "ffmpeg");
            Files.deleteIfExists(ffmpegZip);

            String ffprobeUrl = baseUrl + "/getrelease/ffprobe/zip";
            Path ffprobeZip = gamePath.resolve("ffprobe-mac-temp.zip");
            NarutoLoading.LOGGER.info("{}Downloading ffprobe from: {}", NarutoLoading.info(), ffprobeUrl);

            try (InputStream in = new URL(ffprobeUrl).openStream()) {
                Files.copy(in, ffprobeZip, StandardCopyOption.REPLACE_EXISTING);
            }
            Extractor.unzipSingle(ffprobeZip, targetDir, "ffprobe");
            Files.deleteIfExists(ffprobeZip);

            NarutoLoading.LOGGER.info("{}macOS FFmpeg files extracted to: {}", NarutoLoading.info(), targetDir);
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

        static void unzipSingle(Path zip, Path targetDir, String executableName) throws Exception {
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        Path out = targetDir.resolve(executableName);
                        Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);

                        boolean executable = out.toFile().setExecutable(true, false);
                        NarutoLoading.LOGGER.info("{}Extracted {} to {} (executable: {})", NarutoLoading.info(), executableName, out, executable);
                        break;
                    }
                }
            }
        }
    }

    enum OSType {
        WINDOWS("ffmpeg-win"),
        LINUX("ffmpeg-linux"),
        MACOS("ffmpeg-mac");

        static final OSType CURRENT = current();

        final String dirName;

        OSType(String dirName) {
            this.dirName = dirName;
        }

        static @Nullable FFmpegProvider.OSType current() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) return WINDOWS;
            if (os.contains("mac")) return MACOS;
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