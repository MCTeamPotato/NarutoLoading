package me.kall.narutoloading.common.env.ffmpeg;

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
        File sourceFile = new File(this.absoluteSourcePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) return;

        String fileName = sourceFile.getName();
        String lowerName = fileName.toLowerCase();

        Path parentDir = sourceFile.toPath().getParent();
        File absoluteOutputPath = parentDir.resolve(fileName.substring(0, fileName.lastIndexOf(".")) + ".ogg").toFile();

        if (absoluteOutputPath.exists()) {
            if (this.isMono(absoluteOutputPath.getAbsolutePath())) {
                this.converted = absoluteOutputPath.getAbsolutePath();
                onDone.run();
                return;
            } else {
                absoluteOutputPath.delete();
            }
        }

        if (lowerName.endsWith(".ogg")) {
            if (this.isMono(this.absoluteSourcePath)) {
                this.converted = this.absoluteSourcePath;
                onDone.run();
                return;
            }
        }

        this.converter.submit(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(this.absoluteFFmpegPath, "-i", this.absoluteSourcePath, "-vn", "-acodec", "libvorbis", "-ac", "1", "-q:a", "4", "-y", absoluteOutputPath.getAbsolutePath()).redirectErrorStream(true);

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
                    this.converted = absoluteOutputPath.getAbsolutePath();
                }
            } catch (Exception ignored) {} finally {
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
                return isMono;
            }
        } catch (Exception e) {
        }

        return false;
    }
}
