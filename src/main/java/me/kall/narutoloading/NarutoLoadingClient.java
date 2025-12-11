package me.kall.narutoloading;

import me.kall.duplicationless.config.JsonConfig;
import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.executor.NarutoAudioExecutor;
import me.kall.narutoloading.executor.NarutoVideoExecutor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NarutoLoadingClient {
    public static final NarutoAudioExecutor AUDIO = new NarutoAudioExecutor();
    public static final NarutoVideoExecutor VIDEO = new NarutoVideoExecutor();
    public static final NarutoRenderer RENDERER = new NarutoRenderer();

    public static final class NarutoConfig {
        private static final JsonConfig CONFIG = JsonConfig.create(NarutoLoading.MOD_ID, "4")
                .put("ffmpegExePath", "D:\\ffmpeg\\bin\\ffmpeg.exe")
                .put("ffprobeExePath", "D:\\ffmpeg\\bin\\ffprobe.exe")
                .put("videoFileName", "naruto.mp4")
                .put("audioFileName", "")
                .put("reloadKey", GLFW.GLFW_KEY_F12)
                .put("maxWidthForExecution", 1280)
                .put("maxHeightForExecution", 720)
                .put("audioVolume", 1.0)
                .initialize();

        public static final int RELOAD = CONFIG.getInt("reloadKey");

        private static final String VIDEO_PATH = FMLLoader.getGamePath().resolve("config").resolve(CONFIG.getString("videoFileName")).toAbsolutePath().toString();
        private static final String AUDIO_PATH = FMLLoader.getGamePath().resolve("config").resolve(CONFIG.getString("audioFileName")).toAbsolutePath().toString();

        public static final String FFPROBE_PATH = CONFIG.getString("ffprobeExePath");
        public static final String FFMPEG_PATH = CONFIG.getString("ffmpegExePath");

        public static final int MAX_WIDTH = CONFIG.getInt("maxWidthForExecution");
        public static final int MAX_HEIGHT = CONFIG.getInt("maxHeightForExecution");

        public static final float VOLUME = CONFIG.getFloat("audioVolume");

        public static String video() {
            return VIDEO_PATH;
        }

        public static String audio() {
            return !AUDIO_PATH.endsWith(".mp3") ? video() : AUDIO_PATH;
        }
    }

    public static final class Constants {
        private static final int FPS = getVideoFrameRate();
        private static final IntSupplier WIDTH = () -> Math.min(Minecraft.getInstance().getWindow().getScreenWidth(), NarutoConfig.MAX_WIDTH);
        private static final IntSupplier HEIGHT = () -> Math.min(Minecraft.getInstance().getWindow().getScreenHeight(), NarutoConfig.MAX_HEIGHT);

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
}
