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
    private final ExecutorService converter = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NarutoAudioConverter");
        thread.setDaemon(true);
        return thread;
    });
    public String converted = "";

    public AudioConverter(String absoluteSourcePath, String absoluteFFmpegPath) {
        this.absoluteSourcePath = absoluteSourcePath;
        this.absoluteFFmpegPath = absoluteFFmpegPath;
    }

    public void setup(Runnable onDone) {
        File sourceFile = new File(absoluteSourcePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) return;

        String fileName = sourceFile.getName();
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".ogg")) {
            this.converted = this.absoluteSourcePath;
            onDone.run();
            return;
        }

        Path parentDir = sourceFile.toPath().getParent();
        File absoluteOutputPath = parentDir.resolve(fileName.substring(0, fileName.lastIndexOf(".")) + ".ogg").toFile();

        if (absoluteOutputPath.exists()) {
            this.converted = absoluteOutputPath.getAbsolutePath();
            onDone.run();
            return;
        }

        this.converter.submit(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(this.absoluteFFmpegPath, "-i", absoluteSourcePath, "-vn", "-acodec", "libvorbis", "-q:a", "4", "-y", absoluteOutputPath.getAbsolutePath()).redirectErrorStream(true);
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
                    NarutoLoading.LOGGER.info("{}Successfully converted to OGG: {}", NarutoLoading.info(), absoluteOutputPath.getAbsolutePath());
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
}
