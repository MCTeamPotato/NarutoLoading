package me.kall.narutoloading.media.ffmpeg;

import me.kall.narutoloading.core.util.Executable;
import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class FFmpeg {
    private static final String VIDEO_ENTRIES = "stream=codec_type,width,height,r_frame_rate,avg_frame_rate,start_time:format=duration";
    private static final String AUDIO_ENTRIES = "stream=codec_type,channels,sample_rate,start_time:format=duration";
    private static final String MONO_ENTRIES = "stream=codec_type,channels";

    private static final String STREAM_CODEC_TYPE = "streams.stream.0.codec_type";
    private static final String STREAM_WIDTH = "streams.stream.0.width";
    private static final String STREAM_HEIGHT = "streams.stream.0.height";
    private static final String STREAM_AVG_FPS = "streams.stream.0.avg_frame_rate";
    private static final String STREAM_R_FPS = "streams.stream.0.r_frame_rate";
    private static final String STREAM_START_TIME = "streams.stream.0.start_time";
    private static final String STREAM_CHANNELS = "streams.stream.0.channels";
    private static final String STREAM_SAMPLE_RATE = "streams.stream.0.sample_rate";
    private static final String FORMAT_DURATION = "format.duration";

    private static final String NOT_AVAILABLE = "N/A";

    public final AtomicReference<@Nullable String> ffmpeg = new AtomicReference<>();
    public final AtomicReference<@Nullable String> ffprobe = new AtomicReference<>();

    public @Nullable VideoArgs deriveVideo(@Nullable String absPathStr) {
        String ffprobe = this.ffprobe.get();
        if (ffprobe == null || absPathStr == null) return null;

        Path absPath = regularFile(absPathStr, "video");
        if (absPath == null) return null;

        Map<String, String> values = this.probe(absPathStr, "v:0", VIDEO_ENTRIES);
        if (values == null) {
            System.err.println("[NarutoLoading] FFprobe " + ffprobe + " returned no data for " + absPath + " or encounters exception during the process.");
            return null;
        }

        if (!"video".equals(values.get(STREAM_CODEC_TYPE))) {
            System.err.println("[NarutoLoading] Video stream is not found in " + absPathStr);
            return null;
        }

        double avg = parseRate(values.get(STREAM_AVG_FPS));
        double fps = avg > 0D && Double.isFinite(avg) ? avg : parseRate(values.get(STREAM_R_FPS));
        if (!(fps > 0D) || !Double.isFinite(fps)) {
            System.err.println("[NarutoLoading] There is no usable frame rate for " + absPathStr);
            return null;
        }

        int width = parseInteger(values, STREAM_WIDTH, -1);
        if (width <= 0) {
            System.err.println("[NarutoLoading] There is no usable width for " + absPathStr);
            return null;
        }

        int height = parseInteger(values, STREAM_HEIGHT, -1);
        if (height <= 0) {
            System.err.println("[NarutoLoading] There is no usable height for " + absPathStr);
            return null;
        }

        double duration = parseDouble(values, FORMAT_DURATION, Double.NaN);
        if (Double.isNaN(duration)) {
            System.err.println("[NarutoLoading] There is no usable duration for " + absPathStr);
            return null;
        }

        double startSec = parseDouble(values, STREAM_START_TIME, 0D);
        return new VideoArgs(absPathStr, fps, width, height, duration, startSec);
    }

    public @Nullable AudioArgs deriveAudio(@Nullable String absPathStr) {
        String ffprobe = this.ffprobe.get();
        if (ffprobe == null || absPathStr == null) return null;

        Path absPath = regularFile(absPathStr, "audio");
        if (absPath == null) return null;

        Map<String, String> values = this.probe(absPathStr, "a:0", AUDIO_ENTRIES);
        if (values == null) {
            System.err.println("[NarutoLoading] FFprobe " + ffprobe + " returned no data for " + absPath + " or encounters exception during the process.");
            return null;
        }

        if (!"audio".equals(values.get(STREAM_CODEC_TYPE))) {
            System.err.println("[NarutoLoading] Audio stream is not found in " + absPathStr);
            return null;
        }

        int channelCount = parseInteger(values, STREAM_CHANNELS, -1);
        if (channelCount <= 0) {
            System.err.println("[NarutoLoading] There is no usable channel count for " + absPathStr);
            return null;
        }

        int sampleRate = parseInteger(values, STREAM_SAMPLE_RATE, -1);
        if (sampleRate <= 0) {
            System.err.println("[NarutoLoading] There is no usable sample rate for " + absPathStr);
            return null;
        }

        if (channelCount > 2) {
            System.err.println("[NarutoLoading] Unsupported OpenAL format for " + channelCount + " channel(s) in " + absPathStr);
            return null;
        }

        double duration = parseDouble(values, FORMAT_DURATION, Double.NaN);
        if (Double.isNaN(duration)) {
            System.err.println("[NarutoLoading] There is no usable duration for " + absPathStr);
            return null;
        }

        double startSec = parseDouble(values, STREAM_START_TIME, 0D);
        return new AudioArgs(absPathStr, channelCount, sampleRate, startSec, duration);
    }

    public @Nullable String monoize(@Nullable String absPathStr) {
        String ffmpeg = this.ffmpeg.get();
        if (absPathStr == null || ffmpeg == null) return null;

        Path absPath = regularFile(absPathStr, "audio");
        if (absPath == null) return null;

        if ("ogg".equalsIgnoreCase(FilenameUtils.getExtension(absPathStr)) && this.monoCheck(absPathStr)) return absPathStr;

        String name = FilenameUtils.getBaseName(absPathStr);

        File absOutput = absPath.getParent().resolve(name + ".ogg").toFile();
        String absOutputStr = absOutput.getAbsolutePath();

        if (absOutput.exists()) {
            if (this.monoCheck(absOutputStr)) return absOutputStr;

            if (!absOutput.delete()) {
                System.err.println("[NarutoLoading] Error deleting stale mono audio output " + absOutputStr);
                return null;
            }
        }

        return Executable.runCommand(ffmpeg, "-i", absPathStr, "-vn", "-acodec", "libvorbis", "-ac", "1", "-q:a", "4", "-y", absOutputStr) != null ? absOutputStr : null;
    }

    private static @Nullable Path regularFile(@NotNull String absPathStr, @NotNull String kind) {
        Path absPath;
        try {
            absPath = Path.of(absPathStr);
        } catch (InvalidPathException exception) {
            System.err.println("[NarutoLoading] " + absPathStr + " path string cannot be converted to a Path object: " + exception.getMessage());
            return null;
        }

        if (!Files.isRegularFile(absPath)) {
            System.err.println("[NarutoLoading] " + absPathStr + " does not exist or is not a valid regular " + kind + " file.");
            return null;
        }

        return absPath;
    }

    private boolean monoCheck(String absPathStr) {
        if (this.ffprobe.get() == null) return false;
        Map<String, String> values = this.probe(absPathStr, "a:0", MONO_ENTRIES);
        return values != null && "audio".equals(values.get(STREAM_CODEC_TYPE)) && parseInteger(values, STREAM_CHANNELS, -1) == 1;
    }

    private @Nullable Map<String, String> probe(@NotNull String absPathStr, @NotNull String selector, @NotNull String entries) {
        String ffprobe = this.ffprobe.get();
        if (ffprobe == null) return null;

        String output = Executable.runCommand(ffprobe, "-v", "quiet", "-select_streams", selector, "-show_entries", entries, "-of", "flat", absPathStr);
        if (output == null || output.isEmpty()) return null;

        Map<String, String> values = new HashMap<>();
        for (String line : output.split("\n")) {
            int separator = line.indexOf('=');
            if (separator <= 0) continue;

            String value = line.substring(separator + 1);
            if (value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                value = value.substring(1, value.length() - 1);
            }

            values.put(line.substring(0, separator), value);
        }

        return values.isEmpty() ? null : values;
    }

    @SuppressWarnings("SameParameterValue")
    private static int parseInteger(@NotNull Map<String, String> values, @NotNull String key, int fallback) {
        String value = values.get(key);
        if (value == null || value.equals(NOT_AVAILABLE)) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double parseDouble(@NotNull Map<String, String> values, @NotNull String key, double fallback) {
        String value = values.get(key);
        if (value == null || value.equals(NOT_AVAILABLE)) return fallback;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double parseRate(@Nullable String value) {
        if (value == null || value.equals(NOT_AVAILABLE)) return Double.NaN;

        int slash = value.indexOf('/');
        if (slash <= 0) return Double.NaN;

        try {
            double denominator = Double.parseDouble(value.substring(slash + 1));
            return denominator == 0D ? Double.NaN : Double.parseDouble(value.substring(0, slash)) / denominator;
        } catch (NumberFormatException exception) {
            return Double.NaN;
        }
    }
}
