package me.kall.narutoloading.config;

import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.config.api.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

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
    public static final String THEME = "theme";
    public static final String SHIMMER = "shimmer";
    public static final String SHIMMER_SPEED = "shimmerSpeed";
    public static final String SHIMMER_SCALE = "shimmerScale";
    public static final String SHIMMER_STRENGTH = "shimmerStrength";
    public static final String SHIMMER_FPS = "shimmerFps";
    public static final String FADABLE = "fadable";
    public static final String TICKS_BEFORE_FADE = "ticksBeforeFade";
    public static final String FADE_OUT_STEP = "fadeOutStep";
    public static final String FADE_IN_STEP = "fadeInStep";
    public static final String WORLD_MAX_WIDTH = "worldMaxWidth";
    public static final String WORLD_MAX_HEIGHT = "worldMaxHeight";
    public static final String WORLD_BUFFER_SECONDS = "worldBufferSeconds";
    public static final String WORLD_ACTIVE_DISTANCE = "worldActiveDistance";
    public static final String WORLD_MAX_ACTIVE = "worldMaxActive";
    public static final String WORLD_CULL_INVISIBLE = "worldCullInvisible";
    public static final String WORLD_CULL_FACES = "worldCullFaces";
    public static final String WORLD_LIGHT_ENABLED = "worldLightEnabled";
    public static final String WORLD_AUDIO_ENABLED = "worldAudioEnabled";
    public static final String WORLD_VOLUME = "worldVolume";
    public static final String WORLD_AUDIO_MAX_DISTANCE = "worldAudioMaxDistance";
    public static final String WORLD_MAX_AUDIO = "worldMaxAudio";

    private static final Config CONFIG = create();

    private static @NotNull Config create() {
        try {
            return defaults(Config.builder().file("config/narutoloading.properties")).autoCreate(true).build();
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading] Exception loading the config file, falling back to in-memory defaults: " + throwable.getMessage());
            return defaults(Config.builder()).build();
        }
    }

    private static @NotNull Config.Builder defaults(@NotNull Config.Builder builder) {
        builder.prefix("narutoloading.");

        for (Setting<?> setting : NarutoSettings.ALL) builder.defaultValue(setting.key(), setting.encodedDefault());

        return builder;
    }

    public static int limitWidth() {
        return NarutoSettings.MAX_VIDEO_WIDTH.read(CONFIG);
    }

    public static int limitHeight() {
        return NarutoSettings.MAX_VIDEO_HEIGHT.read(CONFIG);
    }

    public static float volume() {
        return NarutoSettings.AUDIO_VOLUME.read(CONFIG);
    }

    public static @NotNull Path ffmpegDir() {
        return Path.of(NarutoSettings.FFMPEG_DIR.read(CONFIG));
    }

    public static @NotNull Path ytDlpDir() {
        return Path.of(NarutoSettings.YTDLP_DIR.read(CONFIG));
    }

    public static @NotNull Path sourcesDir() {
        return Path.of(NarutoSettings.SOURCES_DIR.read(CONFIG));
    }

    public static double bufferSeconds() {
        return NarutoSettings.BUFFER_SECONDS.read(CONFIG);
    }

    public static MediaRepository.@NotNull Mode selectMode() {
        return NarutoSettings.SELECT_MODE.read(CONFIG);
    }

    public static @NotNull String fixedName() {
        return NarutoSettings.FIXED_NAME.read(CONFIG);
    }

    public static boolean muteMusic() {
        return NarutoSettings.MUTE_MUSIC.read(CONFIG);
    }

    public static boolean debugTimeCosts() {
        return NarutoSettings.DEBUG_TIME_COSTS.read(CONFIG);
    }

    public static int debugTimeCostsInterval() {
        return NarutoSettings.DEBUG_TIME_COSTS_INTERVAL.read(CONFIG);
    }

    public static boolean shimmer() {
        return NarutoSettings.SHIMMER.read(CONFIG);
    }

    public static double shimmerSpeed() {
        return NarutoSettings.SHIMMER_SPEED.read(CONFIG);
    }

    public static double shimmerScale() {
        return NarutoSettings.SHIMMER_SCALE.read(CONFIG);
    }

    public static double shimmerStrength() {
        return NarutoSettings.SHIMMER_STRENGTH.read(CONFIG);
    }

    public static int shimmerFps() {
        return NarutoSettings.SHIMMER_FPS.read(CONFIG);
    }

    public static boolean fadable() {
        return NarutoSettings.FADABLE.read(CONFIG);
    }

    public static int ticksBeforeFade() {
        return NarutoSettings.TICKS_BEFORE_FADE.read(CONFIG);
    }

    public static int fadeOutStep() {
        return NarutoSettings.FADE_OUT_STEP.read(CONFIG);
    }

    public static int fadeInStep() {
        return NarutoSettings.FADE_IN_STEP.read(CONFIG);
    }

    public static int worldLimitWidth() {
        return NarutoSettings.WORLD_MAX_WIDTH.read(CONFIG);
    }

    public static int worldLimitHeight() {
        return NarutoSettings.WORLD_MAX_HEIGHT.read(CONFIG);
    }

    public static double worldBufferSeconds() {
        return NarutoSettings.WORLD_BUFFER_SECONDS.read(CONFIG);
    }

    public static int worldActiveDistance() {
        return NarutoSettings.WORLD_ACTIVE_DISTANCE.read(CONFIG);
    }

    public static int worldMaxActive() {
        return NarutoSettings.WORLD_MAX_ACTIVE.read(CONFIG);
    }

    public static boolean worldCullInvisible() {
        return NarutoSettings.WORLD_CULL_INVISIBLE.read(CONFIG);
    }

    public static boolean cullFaces() {
        return NarutoSettings.WORLD_CULL_FACES.read(CONFIG);
    }

    public static boolean worldLightEnabled() {
        return NarutoSettings.WORLD_LIGHT_ENABLED.read(CONFIG);
    }

    public static boolean worldAudioEnabled() {
        return NarutoSettings.WORLD_AUDIO_ENABLED.read(CONFIG);
    }

    public static float worldVolume() {
        return NarutoSettings.WORLD_VOLUME.read(CONFIG);
    }

    public static int worldAudioMaxDistance() {
        return NarutoSettings.WORLD_AUDIO_MAX_DISTANCE.read(CONFIG);
    }

    public static int worldMaxAudio() {
        return NarutoSettings.WORLD_MAX_AUDIO.read(CONFIG);
    }

    public static @NotNull Map<Setting<?>, Object> snapshot() {
        Map<Setting<?>, Object> values = new LinkedHashMap<>();

        for (Setting<?> setting : NarutoSettings.ALL) values.put(setting, read(setting));

        return values;
    }

    public static void writeAll(@NotNull Map<Setting<?>, Object> values) {
        for (Map.Entry<Setting<?>, Object> entry : values.entrySet()) write(entry.getKey(), entry.getValue());
    }

    public static void set(@NotNull String key, @Nullable Object value) {
        CONFIG.set(key, value);
    }

    public static void save() {
        CONFIG.save();
    }

    private static <T> @NotNull T read(@NotNull Setting<T> setting) {
        return setting.read(CONFIG);
    }

    @SuppressWarnings("unchecked")
    private static <T> void write(@NotNull Setting<T> setting, @Nullable Object value) {
        setting.write(CONFIG, (T) value);
    }
}
