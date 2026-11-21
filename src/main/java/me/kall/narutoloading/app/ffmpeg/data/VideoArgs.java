package me.kall.narutoloading.app.ffmpeg.data;

import me.kall.narutoloading.config.NarutoConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public record VideoArgs(String absPath, double fps, int width, int height, double duration, double startSec) {
    @Override
    public int width() {
        return this.limit(this.width);
    }

    @Override
    public int height() {
        return this.limit(this.height);
    }

    private int limit(double size) {
        double scale = Math.min(1D, Math.min((double) NarutoConfig.limitWidth() / (double) this.width, (double) NarutoConfig.limitHeight() / (double) this.height));
        return ((int) (size * scale)) & ~1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VideoArgs other)) return false;
        return Double.compare(this.fps, other.fps) == 0 && this.width == other.width && this.height == other.height && Double.compare(this.duration, other.duration) == 0 && Double.compare(this.startSec, other.startSec) == 0 && Objects.equals(this.absPath, other.absPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.absPath, this.fps, this.width, this.height, this.duration, this.startSec);
    }

    @Override
    public @NotNull String toString() {
        return "absPath:" + Base64.getEncoder().encodeToString(this.absPath.getBytes(StandardCharsets.UTF_8))
                + ",fps:" + this.fps
                + ",width:" + this.width
                + ",height:" + this.height
                + ",duration:" + this.duration
                + ",startSec:" + this.startSec;
    }

    public static @Nullable VideoArgs fromString(String videoArgsStr) {
        try {
            String[] args = videoArgsStr.split(",");
            return new VideoArgs(
                    new String(Base64.getDecoder().decode(args[0].split(":")[1]), StandardCharsets.UTF_8),
                    Double.parseDouble(args[1].split(":")[1]),
                    Integer.parseInt(args[2].split(":")[1]),
                    Integer.parseInt(args[3].split(":")[1]),
                    Double.parseDouble(args[4].split(":")[1]),
                    Double.parseDouble(args[5].split(":")[1])
            );
        } catch (Exception exception) {
            System.err.println("[NarutoLoading] Exception parsing video args " + videoArgsStr + ": " + exception.getMessage());
            return null;
        }
    }
}
