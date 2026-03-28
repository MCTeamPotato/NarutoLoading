package me.kall.narutoloading.app.ffmpeg;

import me.kall.narutoloading.app.Executable;
import me.kall.narutoloading.data.Paths;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpeg {
    private static final Pattern VIDEO_STREAM = Pattern.compile("\\{[^}]*\"codec_type\"\\s*:\\s*\"video\"[^}]*}");
    private static final Pattern R_FPS = Pattern.compile("\"r_frame_rate\"\\s*:\\s*\"(\\d+)/(\\d+)\"");
    private static final Pattern AVG_FPS = Pattern.compile("\"avg_frame_rate\"\\s*:\\s*\"(\\d+)/(\\d+)\"");
    private static final Pattern FORMAT_DURATION = Pattern.compile("\"format\"\\s*:\\s*\\{[^}]*\"duration\"\\s*:\\s*\"([0-9.]+)\"");

    public static double getFps(String json) {
        Matcher videoMatcher = VIDEO_STREAM.matcher(json);

        if (!videoMatcher.find()) throw new RuntimeException("Matcher#find failed for " + VIDEO_STREAM);

        String videoBlock = videoMatcher.group();

        double fps = parseFraction(videoBlock, R_FPS);
        if (fps > 0) return fps;

        fps = parseFraction(videoBlock, AVG_FPS);
        if (fps > 0) {
            return fps;
        } else {
            throw new RuntimeException("Invalid fps " + fps);
        }
    }

    private static double parseFraction(String text, @NotNull Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) throw new RuntimeException("Matcher#find failed for " + pattern);

        double num = Double.parseDouble(matcher.group(1));
        double den = Double.parseDouble(matcher.group(2));

        if (den == 0) throw new RuntimeException("Invalid den: 0");
        return num / den;
    }

    public static double getDuration(String json) {
        Matcher matcher = FORMAT_DURATION.matcher(json);
        if (matcher.find()) return Double.parseDouble(matcher.group(1)) * 1000;
        return -1;
    }

    public static @NotNull String genJson(String video) {
        return Executable.executeCommand(new String[]{Paths.FFPROBE.toString(), "-v", "quiet", "-print_format", "json", "-show_streams", "-show_format", video}, false);
    }

    public static boolean isMono(String audioPath) {
        return "1".equals(Executable.executeCommand(new String[]{Paths.FFPROBE.toString(), "-v", "error", "-select_streams", "a:0", "-show_entries", "stream=channels", "-of", "default=noprint_wrappers=1:nokey=1", audioPath}, false).trim());
    }

    public static boolean convertToMonoOgg(String inputPath, String outputPath) {
        try {
            System.out.println(Executable.executeCommand(new String[]{Paths.FFMPEG.toString(), "-i", inputPath, "-vn", "-acodec", "libvorbis", "-ac", "1", "-q:a", "4", "-y", outputPath}, false));
            return new File(outputPath).exists();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}