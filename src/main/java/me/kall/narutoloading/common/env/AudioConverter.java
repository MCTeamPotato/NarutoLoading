package me.kall.narutoloading.common.env;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.kall.narutoloading.NarutoLoading;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

public class AudioConverter {

    public static @Nullable String convertToOgg(@NotNull String absoluteSourcePath, @NotNull String ffmpegPath) {
        File sourceFile = new File(absoluteSourcePath);

        if (!sourceFile.exists() || !sourceFile.isFile()) {
            NarutoLoading.LOGGER.error("Source file does not exist: {}", absoluteSourcePath);
            return null;
        }

        String fileName = sourceFile.getName();
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".ogg")) {
            NarutoLoading.LOGGER.info("File is already in OGG format: {}", absoluteSourcePath);
            return absoluteSourcePath;
        }

        Path parentDir = sourceFile.toPath().getParent();
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        String outputFileName = nameWithoutExt + ".ogg";
        File outputFile = parentDir.resolve(outputFileName).toFile();

        if (outputFile.exists()) {
            NarutoLoading.LOGGER.info("OGG file already exists: {}", outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();
        }

        try {
            NarutoLoading.LOGGER.info("Converting {} to OGG format...", absoluteSourcePath);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    ffmpegPath,
                    "-i", absoluteSourcePath,
                    "-vn",
                    "-acodec", "libvorbis",
                    "-q:a", "4",
                    "-y",
                    outputFile.getAbsolutePath()
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0 && outputFile.exists()) {
                NarutoLoading.LOGGER.info("Successfully converted to OGG: {}", outputFile.getAbsolutePath());
                return outputFile.getAbsolutePath();
            } else {
                NarutoLoading.LOGGER.error("FFmpeg conversion failed with exit code: {}", exitCode);
                NarutoLoading.LOGGER.error("FFmpeg output:\n{}", output.toString());
                return null;
            }

        } catch (Exception e) {
            NarutoLoading.LOGGER.error("Error converting file to OGG: {}", absoluteSourcePath, e);
            return null;
        }
    }

    public static @NotNull List<String> batchConvertToOgg(@NotNull List<String> absoluteSourcePaths, @NotNull String ffmpegPath) {
        List<String> converted = new ObjectArrayList<>(absoluteSourcePaths.size());

        for (String absoluteSourcePath : absoluteSourcePaths) {
            converted.add(convertToOgg(absoluteSourcePath, ffmpegPath));
        }

        return converted;
    }

    public static boolean deleteConvertedOgg(@NotNull String oggFilePath) {
        File file = new File(oggFilePath);
        if (file.exists() && file.getName().toLowerCase().endsWith(".ogg")) {
            boolean deleted = file.delete();
            if (deleted) NarutoLoading.LOGGER.info("Deleted OGG file: {}", oggFilePath);
            return deleted;
        }
        return false;
    }
}