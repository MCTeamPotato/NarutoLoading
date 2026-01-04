package me.kall.narutoloading.common.env.ffmpeg;

import me.kall.narutoloading.NarutoLoading;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoArgReader {
    private double fps = 0D;
    private long duration = 0L;

    private final String video;
    private final String ffprobe;

    public VideoArgReader(String absoluteVideoPath, String absoluteFFprobePath) {
        this.video = absoluteVideoPath;
        this.ffprobe = absoluteFFprobePath;
        this.setup();
    }

    public void setup() {
        NarutoLoading.LOGGER.info("{}Start to use [{}] to read video [{}] arguments", NarutoLoading.info(), this.ffprobe, this.video);
        String json = FFprobe.genJson(this.video, this.ffprobe);
        this.fps = FFprobe.getFps(json);
        this.duration = FFprobe.getDuration(json);
        NarutoLoading.LOGGER.info("{}NarutoLoading video fps: {}", NarutoLoading.info(), this.fps);
        NarutoLoading.LOGGER.info("{}NarutoLoading video duration: {}", NarutoLoading.info(), this.duration);
    }

    public double fps() {
        return this.fps;
    }

    public long duration() {
        return this.duration;
    }

    static class FFprobe {
        static final Pattern FPS = Pattern.compile("\"avg_frame_rate\"\\s*:\\s*\"(\\d+)/(\\d+)\"");
        static final Pattern DURATION = Pattern.compile("\"duration\"\\s*:\\s*\"([0-9.]+)\"");

        static double getFps(String json) {
            if (json != null) {
                Matcher matcher = FPS.matcher(json);

                if (matcher.find()) {
                    double num = Double.parseDouble(matcher.group(1));
                    double den = Double.parseDouble(matcher.group(2));
                    if (den != 0) {
                        return num / den;
                    }
                }
            }
            throw new RuntimeException("Failed to read video frame rate");
        }

        static long getDuration(String json) {
            if (json != null) {
                Matcher matcher = DURATION.matcher(json);

                if (matcher.find()) return (long) (Double.parseDouble(matcher.group(1)) * 1000);
            }
            throw new RuntimeException("Failed to read video duration");
        }

        static @Nullable String genJson(String video, String ffprobe) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(ffprobe, "-v", "quiet", "-print_format", "json", "-show_streams", "-show_format", video).redirectErrorStream(true);

                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) stringBuilder.append(line).append('\n');
                process.waitFor();
                return stringBuilder.toString();
            } catch (Exception exception) {
                return null;
            }
        }
    }
}
