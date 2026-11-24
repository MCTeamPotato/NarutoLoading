package me.kall.narutoloading.media.ffmpeg.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public record AudioArgs(String absPath, int channels, int sampleRate, double startSec, double duration) {
    public AudioArgs(String absPath, int channels, int sampleRate, double startSec, double duration) {
        this.absPath = absPath;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.startSec = startSec;
        this.duration = duration;

        System.out.println("[NarutoLoading] AudioArgs created. Path: " + this.absPath + ". Channels: " + this.channels + ". Sample Rate: " + this.sampleRate + ". Duration: " + this.duration + " seconds. Start Offset: " + this.startSec + " seconds.");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AudioArgs other)) return false;
        return this.channels == other.channels && this.sampleRate == other.sampleRate && Double.compare(this.startSec, other.startSec) == 0 && Double.compare(this.duration, other.duration) == 0 && Objects.equals(this.absPath, other.absPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.absPath, this.channels, this.sampleRate, this.startSec, this.duration);
    }

    @Override
    public @NotNull String toString() {
        return "absPath:" + Base64.getEncoder().encodeToString(this.absPath.getBytes(StandardCharsets.UTF_8))
                + ",channels:" + this.channels
                + ",sampleRate:" + this.sampleRate
                + ",startSec:" + this.startSec
                + ",duration:" + this.duration;
    }

    public static @Nullable AudioArgs fromString(String audioArgsStr) {
        try {
            String[] args = audioArgsStr.split(",");
            return new AudioArgs(
                    new String(Base64.getDecoder().decode(args[0].split(":")[1]), StandardCharsets.UTF_8),
                    Integer.parseInt(args[1].split(":")[1]),
                    Integer.parseInt(args[2].split(":")[1]),
                    Double.parseDouble(args[3].split(":")[1]),
                    Double.parseDouble(args[4].split(":")[1])
            );
        } catch (Exception exception) {
            System.err.println("[NarutoLoading] Exception parsing audio args " + audioArgsStr + ": " + exception.getMessage());
            return null;
        }
    }
}
