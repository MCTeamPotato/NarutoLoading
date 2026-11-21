package me.kall.narutoloading.config;

import me.kall.narutoloading.app.MediaRepository;
import me.kall.narutoloading.config.api.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class NarutoConfig {
    public static final String MAX_VIDEO_WIDTH = "maxVideoWidth";
    public static final String MAX_VIDEO_HEIGHT = "maxVideoHeight";
    public static final String AUDIO_VOLUME = "audioVolume";
    public static final String FFMPEG_DIR = "ffmpegDir";
    public static final String YTDLP_DIR = "ytDlpDir";
    public static final String SOURCES_DIR = "sourcesDir";
    public static final String BUFFER_SECONDS = "bufferSeconds";
    public static final String SELECT_MODE = "selectMode";
    public static final String FIXED_NAME = "fixedName";
    public static final String MUTE_MUSIC = "muteMusic";
    public static final String DEBUG_TIME_COSTS = "debugTimeCosts";
    public static final String DEBUG_TIME_COSTS_INTERVAL = "debugTimeCostsInterval";
    public static final String SHIMMER = "shimmer";
    public static final String SHIMMER_SPEED = "shimmerSpeed";
    public static final String SHIMMER_SCALE = "shimmerScale";
    public static final String SHIMMER_STRENGTH = "shimmerStrength";
    public static final String SHIMMER_FPS = "shimmerFps";
    public static final String FADABLE = "fadable";
    public static final String TICKS_BEFORE_FADE = "ticksBeforeFade";

    private static final Config CONFIG = create();

    private static @NotNull Config create() {
        try {
            return defaults(Config.builder().file("config/narutoloading.properties")).autoCreate(true).build();
        } catch (Throwable throwable) {
            // A broken or read-only config file must not take the game down with it: the static
            // initialiser runs on the render thread, so an exception here is a hard crash.
            System.err.println("[NarutoLoading] Exception loading the config file, falling back to in-memory defaults: " + throwable.getMessage());
            return defaults(Config.builder()).build();
        }
    }

    private static @NotNull Config.Builder defaults(@NotNull Config.Builder builder) {
        return builder.prefix("narutoloading.")
                .defaultValue(MAX_VIDEO_WIDTH, 1920)
                .defaultValue(MAX_VIDEO_HEIGHT, 1080)
                .defaultValue(AUDIO_VOLUME, 1.0F)
                .defaultValue(FFMPEG_DIR, "")
                .defaultValue(YTDLP_DIR, "")
                .defaultValue(SOURCES_DIR, "narutoloading")
                .defaultValue(BUFFER_SECONDS, 1D)
                .defaultValue(SELECT_MODE, "random")
                .defaultValue(FIXED_NAME, "")
                .defaultValue(MUTE_MUSIC, true)
                .defaultValue(DEBUG_TIME_COSTS, false)
                .defaultValue(DEBUG_TIME_COSTS_INTERVAL, 3)
                .defaultValue(SHIMMER, true)
                .defaultValue(SHIMMER_SPEED, 1.0D)
                .defaultValue(SHIMMER_SCALE, 1.0D)
                .defaultValue(SHIMMER_STRENGTH, 1.0D)
                .defaultValue(SHIMMER_FPS, 30)
                .defaultValue(FADABLE, true)
                .defaultValue(TICKS_BEFORE_FADE, 100);
    }

    public static int limitWidth() {
        return CONFIG.getInt(MAX_VIDEO_WIDTH, Integer.MAX_VALUE);
    }

    public static int limitHeight() {
        return CONFIG.getInt(MAX_VIDEO_HEIGHT, Integer.MAX_VALUE);
    }

    public static float volume() {
        return (float) CONFIG.getDouble(AUDIO_VOLUME, 1.0D);
    }

    public static @NotNull Path ffmpegDir() {
        return Path.of(CONFIG.getString(FFMPEG_DIR, ""));
    }

    public static @NotNull Path ytDlpDir() {
        return Path.of(CONFIG.getString(YTDLP_DIR, ""));
    }

    public static @NotNull Path sourcesDir() {
        return Path.of(CONFIG.getString(SOURCES_DIR, "narutoloading"));
    }

    public static double bufferSeconds() {
        return CONFIG.getDouble(BUFFER_SECONDS, 1D);
    }

    public static MediaRepository.@NotNull Mode selectMode() {
        return MediaRepository.Mode.parse(CONFIG.getString(SELECT_MODE, "random"));
    }

    public static String fixedName() {
        return CONFIG.getString(FIXED_NAME, "");
    }

    public static boolean muteMusic() {
        return CONFIG.getBoolean(MUTE_MUSIC, false);
    }

    public static boolean debugTimeCosts() {
        return CONFIG.getBoolean(DEBUG_TIME_COSTS, false);
    }

    public static int debugTimeCostsInterval() {
        return CONFIG.getInt(DEBUG_TIME_COSTS_INTERVAL, 10);
    }

    public static boolean shimmer() {
        return CONFIG.getBoolean(SHIMMER, true);
    }

    public static double shimmerSpeed() {
        return CONFIG.getDouble(SHIMMER_SPEED, 1.0D);
    }

    public static double shimmerScale() {
        return CONFIG.getDouble(SHIMMER_SCALE, 1.0D);
    }

    public static double shimmerStrength() {
        return CONFIG.getDouble(SHIMMER_STRENGTH, 1.0D);
    }

    public static int shimmerFps() {
        return CONFIG.getInt(SHIMMER_FPS, 30);
    }

    public static boolean fadable() {
        return CONFIG.getBoolean(FADABLE, true);
    }

    public static int ticksBeforeFade() {
        return CONFIG.getInt(TICKS_BEFORE_FADE, 100);
    }

    public static void set(@NotNull String key, @Nullable Object value) {
        CONFIG.set(key, value);
    }

    public static void save() {
        CONFIG.save();
    }
}
