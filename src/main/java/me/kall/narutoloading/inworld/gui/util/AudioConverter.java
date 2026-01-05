package me.kall.narutoloading.inworld.gui.util;

import me.kall.narutoloading.NarutoLoading;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioConverter {
    private final String absoluteSourcePath;
    private final String absoluteFFmpegPath;
    private final String absoluteFFprobePath;
    private final ExecutorService converter = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NarutoAudioConverter");
        thread.setDaemon(true);
        return thread;
    });
    public String converted = NarutoLoading.BLANK;

    public AudioConverter(String absoluteSourcePath, String absoluteFFmpegPath, String absoluteFFprobePath) {
        this.absoluteSourcePath = absoluteSourcePath;
        this.absoluteFFmpegPath = absoluteFFmpegPath;
        this.absoluteFFprobePath = absoluteFFprobePath;
    }

    public void setup(Runnable onDone) {
        File sourceFile = new File(absoluteSourcePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) return;

        String fileName = sourceFile.getName();
        String lowerName = fileName.toLowerCase();

        Path parentDir = sourceFile.toPath().getParent();
        File absoluteOutputPath = parentDir.resolve(fileName.substring(0, fileName.lastIndexOf(".")) + ".ogg").toFile();

        if (absoluteOutputPath.exists()) {
            if (isMono(absoluteOutputPath.getAbsolutePath())) {
                this.converted = absoluteOutputPath.getAbsolutePath();
                onDone.run();
                return;
            } else {
                if (absoluteOutputPath.delete()) {
                    NarutoLoading.LOGGER.info("{}Existing OGG is not mono, reconverting: {}", NarutoLoading.info(), absoluteOutputPath.getAbsolutePath());
                }
            }
        }

        if (lowerName.endsWith(".ogg")) {
            if (isMono(this.absoluteSourcePath)) {
                this.converted = this.absoluteSourcePath;
                onDone.run();
                return;
            } else {
                NarutoLoading.LOGGER.info("{}Source OGG is stereo, converting to mono: {}", NarutoLoading.info(), this.absoluteSourcePath);
            }
        }

        this.converter.submit(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(this.absoluteFFmpegPath, "-i", absoluteSourcePath, "-vn", "-acodec", "libvorbis", "-ac", "1", "-q:a", "4", "-y", absoluteOutputPath.getAbsolutePath()).redirectErrorStream(true);

                Process process = processBuilder.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();

                if (exitCode == 0 && absoluteOutputPath.exists()) {
                    NarutoLoading.LOGGER.info("{}Successfully converted to mono OGG: {}", NarutoLoading.info(), absoluteOutputPath.getAbsolutePath());
                    this.converted = absoluteOutputPath.getAbsolutePath();
                } else {
                    NarutoLoading.LOGGER.error("FFmpeg conversion failed with exit code: {}", exitCode);
                    NarutoLoading.LOGGER.error("FFmpeg output:\n{}", output.toString());
                }
            } catch (Exception exception) {
                NarutoLoading.LOGGER.error("Error converting audio ", exception);
            } finally {
                onDone.run();
                this.converter.shutdown();
            }
        });
    }

    private boolean isMono(String audioPath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(this.absoluteFFprobePath, "-v", "error", "-select_streams", "a:0", "-show_entries", "stream=channels", "-of", "default=noprint_wrappers=1:nokey=1", audioPath).redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line.trim());
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                String channelCount = output.toString().trim();
                boolean isMono = "1".equals(channelCount);
                NarutoLoading.LOGGER.info("{}Audio file {} has {} channel(s), mono: {}", NarutoLoading.info(), audioPath, channelCount, isMono);
                return isMono;
            }
        } catch (Exception e) {
            NarutoLoading.LOGGER.warn("Failed to check audio channels for {}: {}", audioPath, e.getMessage());
        }

        return false;
    }
}
