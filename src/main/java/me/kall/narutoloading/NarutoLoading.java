package me.kall.narutoloading;

import me.kall.narutoloading.executor.NarutoAudioExecutor;
import me.kall.narutoloading.executor.NarutoVideoExecutor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";

    public static final String FFMPEG_PATH = "D:\\ffmpeg\\bin\\ffmpeg.exe";
    public static final String FFPROBE_PATH = "D:\\ffmpeg\\bin\\ffprobe.exe";

    public static final String VIDEO_PATH = FMLLoader.getGamePath().resolve("config").resolve("naruto.mp4").toAbsolutePath().toString();
    
    private static final int FPS = getVideoFrameRate();

    public static final NarutoAudioExecutor AUDIO = new NarutoAudioExecutor();
    public static final NarutoVideoExecutor VIDEO = new NarutoVideoExecutor();

    public static final NarutoRenderer RENDERER = new NarutoRenderer();

    private static final Supplier<Integer> WIDTH = () -> Minecraft.getInstance().getWindow().getScreenWidth();
    private static final Supplier<Integer> HEIGHT = () -> Minecraft.getInstance().getWindow().getScreenHeight();

    private static final Supplier<String> WIDTH_STRING = () -> String.valueOf(WIDTH.get());
    private static final Supplier<String> HEIGHT_STRING = () -> String.valueOf(HEIGHT.get());

    public static int fps() {
        return FPS;
    }

    public static int width() {
        return WIDTH.get();
    }

    public static int height() {
        return HEIGHT.get();
    }

    public static @NotNull String widthString() {
        return WIDTH_STRING.get();
    }

    public static @NotNull String heightString() {
        return HEIGHT_STRING.get();
    }

    private static int getVideoFrameRate() {
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

    private static @Nullable String run() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(FFPROBE_PATH, "-v", "quiet", "-print_format", "json", "-show_streams", VIDEO_PATH);

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
