package me.kall.narutoloading;

import me.kall.narutoloading.config.NarutoConfig;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Constants {
    private static final int FPS = getVideoFrameRate();
    private static final IntSupplier WIDTH = () -> {
        int width = Minecraft.getInstance().getWindow().getScreenWidth();
        if (width == 0) return 854;
        return width;
    };
    private static final IntSupplier HEIGHT = () -> {
        int height = Minecraft.getInstance().getWindow().getScreenHeight();
        if (height == 0) return 480;
        return height;
    };

    private static final Supplier<String> WIDTH_STRING = () -> String.valueOf(WIDTH.getAsInt());
    private static final Supplier<String> HEIGHT_STRING = () -> String.valueOf(HEIGHT.getAsInt());

    private static final BooleanSupplier WINDOW = () -> Minecraft.getInstance().isWindowActive();

    public static int fps() {
        return FPS;
    }

    public static int width() {
        return WIDTH.getAsInt();
    }

    public static int height() {
        return HEIGHT.getAsInt();
    }

    public static @NotNull String widthString() {
        return WIDTH_STRING.get();
    }

    public static @NotNull String heightString() {
        return HEIGHT_STRING.get();
    }

    public static boolean isWindowActive() {
        return WINDOW.getAsBoolean();
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
            ProcessBuilder processBuilder = new ProcessBuilder(NarutoConfig.FFPROBE_PATH, "-v", "quiet", "-print_format", "json", "-show_streams", NarutoConfig.video());

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
