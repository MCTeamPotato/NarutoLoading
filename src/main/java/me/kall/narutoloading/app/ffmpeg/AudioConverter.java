package me.kall.narutoloading.app.ffmpeg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public class AudioConverter {
    public @Nullable String converted;

    public AudioConverter(@NotNull String absoluteSourcePath) {
        File sourceFile = new File(absoluteSourcePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) throw new UnsupportedOperationException("Invalid source file: " + absoluteSourcePath);

        String sourceFileName = sourceFile.getName();
        String sourceFileLowerName = sourceFileName.toLowerCase();

        Path parentDirectory = sourceFile.toPath().getParent();
        File absoluteOutputPath = parentDirectory.resolve(sourceFileName.substring(0, sourceFileName.lastIndexOf(".")) + ".ogg").toFile();

        if (absoluteOutputPath.exists()) {
            if (FFmpeg.isMono(absoluteOutputPath.getPath())) {
                this.converted = absoluteOutputPath.getAbsolutePath();
                return;
            } else {
                if (absoluteOutputPath.delete()) {
                    System.out.println("Output " + absoluteOutputPath + " is deleted as it's not mono.");
                }
            }
        }

        if (sourceFileLowerName.endsWith(".ogg")) {
            if (FFmpeg.isMono(absoluteSourcePath)) {
                this.converted = absoluteSourcePath;
                return;
            }
        }

        if (FFmpeg.convertToMonoOgg(absoluteSourcePath, absoluteOutputPath.getPath())) {
            this.converted = absoluteOutputPath.getAbsolutePath();
        }
    }
}
