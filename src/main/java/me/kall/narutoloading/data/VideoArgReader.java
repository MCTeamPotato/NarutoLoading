package me.kall.narutoloading.data;

import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoArgReader {
    private static final Pattern FPS = Pattern.compile("\"avg_frame_rate\"\\s*:\\s*\"(\\d+)/(\\d+)\"");
    private static final Pattern DURATION = Pattern.compile("\"duration\"\\s*:\\s*\"([0-9.]+)\"");

    private int fps = 0;
    private long duration = 0L;
    private final NarutoRenderer renderer;

    public VideoArgReader(@NotNull NarutoRenderer renderer) {
        this.renderer = renderer;
    }

    public void setup() {
        String json = run(this.renderer.narutoConfig.video);
        this.fps = getFps(json);
        this.duration = getDuration(json);
    }

    public int fps() {
        return this.fps;
    }

    public long duration() {
        return this.duration;
    }

    public int width() {
        int width = Minecraft.getInstance().getWindow().getScreenWidth();
        if (width > this.renderer.narutoConfig.width) width = this.renderer.narutoConfig.width;
        return width;
    }

    public int height() {
        int height = Minecraft.getInstance().getWindow().getScreenHeight();
        if (height > this.renderer.narutoConfig.height) height = this.renderer.narutoConfig.height;
        return height;
    }

    public @NotNull String widthString() {
        return String.valueOf(width());
    }

    public @NotNull String heightString() {
        return String.valueOf(height());
    }

    private int getFps(String json) {
        if (json != null) {
            Matcher matcher = FPS.matcher(json);

            if (matcher.find()) {
                double num = Double.parseDouble(matcher.group(1));
                double den = Double.parseDouble(matcher.group(2));
                if (den != 0) {
                    return (int) (num / den);
                }
            }
        }
        throw new RuntimeException("Failed to read video frame rate");
    }

    private long getDuration(String json) {
        if (json != null) {
            Matcher matcher = DURATION.matcher(json);

            if (matcher.find()) return (long) (Double.parseDouble(matcher.group(1)) * 1000);
        }
        throw new RuntimeException("Failed to read video duration");
    }

    private @Nullable String run(String video) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(this.renderer.ffmpegProvider.ffprobe, "-v", "quiet", "-print_format", "json", "-show_streams", "-show_format", video);

            processBuilder.redirectErrorStream(true);
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
