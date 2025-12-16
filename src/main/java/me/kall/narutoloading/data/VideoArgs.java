package me.kall.narutoloading.data;

import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.NarutoLoading;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VideoArgs {
    private static final int FPS = getFps();
    private static final long DURATION = getDuration();

    static {
        NarutoLoading.LOGGER.info("[NarutoLoading] Video Fps: {}.", fps());
        NarutoLoading.LOGGER.info("[NarutoLoading] Video duration: {}.", duration());
    }

    public static int fps() {
        return FPS;
    }

    public static long duration() {
        return DURATION;
    }

    public static int width() {
        int width = Minecraft.getInstance().getWindow().getScreenWidth();
        if (width > NarutoConfig.WIDTH) width = NarutoConfig.WIDTH;
        return width;
    }

    public static int height() {
        int height = Minecraft.getInstance().getWindow().getScreenHeight();
        if (height > NarutoConfig.HEIGHT) height = NarutoConfig.HEIGHT;
        return height;
    }

    public static @NotNull String widthString() {
        return String.valueOf(width());
    }

    public static @NotNull String heightString() {
        return String.valueOf(height());
    }

    private static int getFps() {
        String json = run();

        if (json != null) {
            Pattern p = Pattern.compile("\"avg_frame_rate\"\\s*:\\s*\"(\\d+)/(\\d+)\"");
            Matcher m = p.matcher(json);

            if (m.find()) {
                double num = Double.parseDouble(m.group(1));
                double den = Double.parseDouble(m.group(2));
                if (den != 0) {
                    return (int) (num / den);
                }
            }
        }
        throw new RuntimeException("Failed to read video frame rate");
    }

    private static long getDuration() {
        String json = run();

        if (json != null) {
            Pattern p = Pattern.compile("\"duration\"\\s*:\\s*\"([0-9.]+)\"");
            Matcher m = p.matcher(json);

            if (m.find()) {
                return (long) (Double.parseDouble(m.group(1)) * 1000);
            }
        }
        throw new RuntimeException("Failed to read video duration");
    }


    private static @Nullable String run() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    FFmpeg.FFPROBE,
                    "-v", "quiet",
                    "-print_format", "json",
                    "-show_streams", "-show_format",
                    NarutoConfig.video()
            );

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
