package me.kall.narutoloading.data;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoRenderer;
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
    public volatile @Nullable String ffmpeg;
    public volatile @Nullable String ffprobe;

    private boolean fromDownload = false;
    private volatile boolean availability;

    private final NarutoRenderer renderer;

    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task , "NarutoFFmpegDownloader");
        thread.setDaemon(true);
        return thread;
    });

    public FFmpegProvider(NarutoRenderer renderer) {
        this.renderer = renderer;
    }

    public boolean available() {
        if (this.fromDownload) {
            return this.availability;
        } else {
            return true;
        }
    }

    public void init() {
        this.downloadExecutor.submit(() -> {
            boolean downloadSucceed = false;
            String configFFmpeg = validExe(this.renderer.narutoConfig.ffmpegPath);
            String configFFprobe = validExe(this.renderer.narutoConfig.ffprobePath);

            if (configFFmpeg != null && configFFprobe != null) {
                this.ffmpeg = configFFmpeg;
                this.ffprobe = configFFprobe;
                NarutoLoading.LOGGER.info("Using FFmpeg from config.");
                this.renderer.videoArgReader.setup();
                return;
            }

            if ((this.renderer.narutoConfig.ffmpegPath != null && !this.renderer.narutoConfig.ffmpegPath.isBlank()) || (this.renderer.narutoConfig.ffprobePath != null && !NarutoRenderer.INSTANCE.narutoConfig.ffprobePath.isBlank())) {
                NarutoLoading.LOGGER.info("FFmpeg path in config is invalid, deprecate it.");
            }

            OSType os = OSType.CURRENT;
            Path gamePath = FMLLoader.getGamePath();
            String baseDir = getBase(gamePath, os);

            if (baseDir == null && os != null) {
                this.fromDownload = true;
                try {
                    NarutoLoading.LOGGER.info("Downloading FFmpeg...");
                    Downloader downloader = new Downloader(this.renderer);
                    downloader.setup(gamePath, os);
                    baseDir = getBase(gamePath, os);
                    downloadSucceed = true;
                } catch (Exception exception) {
                    NarutoLoading.LOGGER.error("Error downloading FFmpeg.", exception);
                } finally {
                    NarutoLoading.LOGGER.info("FFmpeg download task ends.");
                }
            }

            if (baseDir == null) {
                this.ffmpeg = null;
                this.ffprobe = null;
                return;
            }

            boolean windows = os == OSType.WINDOWS;
            String ffmpegName = windows ? "ffmpeg.exe" : "ffmpeg";
            String ffprobeName = windows ? "ffprobe.exe" : "ffprobe";

            File ffmpegFile = get(baseDir, ffmpegName);
            File ffprobeFile = get(baseDir, ffprobeName);

            this.ffmpeg = ffmpegFile.exists() ? ffmpegFile.getAbsolutePath() : null;
            this.ffprobe = ffprobeFile.exists() ? ffprobeFile.getAbsolutePath() : null;

            this.renderer.videoArgReader.setup();

            if (downloadSucceed) this.availability = true;
        });
    }

    private static @Nullable String validExe(String path) {
        if (path == null || path.isBlank()) return null;
        File file = new File(path);
        return file.exists() ? file.getAbsolutePath() : null;
    }

    private static @Nullable String getBase(Path gamePath, @Nullable OSType os) {
        if (os != null) {
            Path osDir = gamePath.resolve(os.dirName);
            if (osDir.toFile().exists()) {
                return os.dirName;
            }
        }

        return gamePath.resolve("ffmpeg").toFile().exists() ? "ffmpeg" : null;
    }

    private static @NotNull File get(String baseDir, String fileName) {
        return FMLLoader.getGamePath().resolve(baseDir).resolve("bin").resolve(fileName).toFile();
    }

    enum OSType {
        WINDOWS("ffmpeg-win"),
        LINUX("ffmpeg-linux"),
        MAC("ffmpeg-mac");

        static final OSType CURRENT = current();

        final String dirName;

        OSType(String dirName) {
            this.dirName = dirName;
        }

        static @Nullable OSType current() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) return WINDOWS;
            if (os.contains("linux")) return LINUX;
            if (os.contains("mac") || os.contains("darwin")) return MAC;
            return null;
        }
    }

    record Downloader(NarutoRenderer renderer) {

        void setup(Path gamePath, @NotNull OSType os) throws Exception {
            String url = switch (os) {
                case WINDOWS -> this.renderer.narutoConfig.winUrl;
                case LINUX -> this.renderer.narutoConfig.linuxUrl;
                default -> throw new UnsupportedOperationException("Unsupported OS: " + os);
            };

            Path tmp = gamePath.resolve("ffmpeg-download.tmp");
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }

            Path targetDir = gamePath.resolve("ffmpeg");

            if (os == OSType.WINDOWS) {
                unzip(tmp, targetDir);
            } else {
                untarXz(tmp, targetDir);
            }

            Files.deleteIfExists(tmp);
        }

        void unzip(Path zip, Path targetDir) throws Exception {
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

        void untarXz(@NotNull Path archive, Path targetDir) throws Exception {
            Files.createDirectories(targetDir);

            Process process = new ProcessBuilder("tar", "-xJf", archive.toAbsolutePath().toString(), "-C", targetDir.toAbsolutePath().toString(), "--strip-components=1").inheritIO().start();

            int code = process.waitFor();
            if (code != 0) {
                throw new RuntimeException("tar failed with exit code " + code);
            }
        }
    }
}
