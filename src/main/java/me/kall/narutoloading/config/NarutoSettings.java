package me.kall.narutoloading.config;

import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.wall.WallRenderMode;

import java.util.List;
import java.util.Locale;

public final class NarutoSettings {
    public static final Setting<Integer> MAX_VIDEO_WIDTH = Setting.integer(NarutoConfig.MAX_VIDEO_WIDTH, 1920, true);
    public static final Setting<Integer> MAX_VIDEO_HEIGHT = Setting.integer(NarutoConfig.MAX_VIDEO_HEIGHT, 1080, true);
    public static final Setting<Float> AUDIO_VOLUME = Setting.single(NarutoConfig.AUDIO_VOLUME, 1.0F, false);
    public static final Setting<String> FFMPEG_DIR = Setting.text(NarutoConfig.FFMPEG_DIR, "", false);
    public static final Setting<String> YTDLP_DIR = Setting.text(NarutoConfig.YTDLP_DIR, "", false);
    public static final Setting<String> SOURCES_DIR = Setting.text(NarutoConfig.SOURCES_DIR, "narutoloading", true);
    public static final Setting<Double> BUFFER_SECONDS = Setting.decimal(NarutoConfig.BUFFER_SECONDS, 1D, true);
    public static final Setting<MediaRepository.Mode> SELECT_MODE = Setting.mapped(NarutoConfig.SELECT_MODE, MediaRepository.Mode.RANDOM, MediaRepository.Mode::parse, mode -> mode.name().toLowerCase(Locale.ROOT), true);
    public static final Setting<String> FIXED_NAME = Setting.text(NarutoConfig.FIXED_NAME, "", true);
    public static final Setting<Boolean> MUTE_MUSIC = Setting.bool(NarutoConfig.MUTE_MUSIC, true, false);
    public static final Setting<Boolean> DEBUG_TIME_COSTS = Setting.bool(NarutoConfig.DEBUG_TIME_COSTS, false, false);
    public static final Setting<Integer> DEBUG_TIME_COSTS_INTERVAL = Setting.integer(NarutoConfig.DEBUG_TIME_COSTS_INTERVAL, 3, false);
    public static final Setting<Boolean> SHIMMER = Setting.bool(NarutoConfig.SHIMMER, true, false);
    public static final Setting<Double> SHIMMER_SPEED = Setting.decimal(NarutoConfig.SHIMMER_SPEED, 1.0D, false);
    public static final Setting<Double> SHIMMER_SCALE = Setting.decimal(NarutoConfig.SHIMMER_SCALE, 1.0D, false);
    public static final Setting<Double> SHIMMER_STRENGTH = Setting.decimal(NarutoConfig.SHIMMER_STRENGTH, 1.0D, false);
    public static final Setting<Integer> SHIMMER_FPS = Setting.integer(NarutoConfig.SHIMMER_FPS, 30, false);
    public static final Setting<Boolean> FADABLE = Setting.bool(NarutoConfig.FADABLE, true, false);
    public static final Setting<Boolean> COMPAT_MODE = Setting.bool(NarutoConfig.COMPAT_MODE, false, false);
    public static final Setting<Integer> TICKS_BEFORE_FADE = Setting.integer(NarutoConfig.TICKS_BEFORE_FADE, 100, false);
    public static final Setting<Integer> FADE_OUT_STEP = Setting.integer(NarutoConfig.FADE_OUT_STEP, 1, false);
    public static final Setting<Integer> FADE_IN_STEP = Setting.integer(NarutoConfig.FADE_IN_STEP, 5, false);
    public static final Setting<WallRenderMode> WORLD_RENDER_MODE = Setting.mapped(NarutoConfig.WORLD_RENDER_MODE, WallRenderMode.AUTO, WallRenderMode::parse, mode -> mode.name().toLowerCase(Locale.ROOT), true);
    public static final Setting<Integer> WORLD_MAX_WIDTH = Setting.integer(NarutoConfig.WORLD_MAX_WIDTH, 1280, true);
    public static final Setting<Integer> WORLD_MAX_HEIGHT = Setting.integer(NarutoConfig.WORLD_MAX_HEIGHT, 720, true);
    public static final Setting<Double> WORLD_BUFFER_SECONDS = Setting.decimal(NarutoConfig.WORLD_BUFFER_SECONDS, 1.0D, true);
    public static final Setting<Integer> WORLD_ACTIVE_DISTANCE = Setting.integer(NarutoConfig.WORLD_ACTIVE_DISTANCE, 48, false);
    public static final Setting<Integer> WORLD_MAX_ACTIVE = Setting.integer(NarutoConfig.WORLD_MAX_ACTIVE, 4, false);
    public static final Setting<Boolean> WORLD_CULL_INVISIBLE = Setting.bool(NarutoConfig.WORLD_CULL_INVISIBLE, true, false);
    public static final Setting<Boolean> WORLD_CULL_FACES = Setting.bool(NarutoConfig.WORLD_CULL_FACES, true, false);
    public static final Setting<Boolean> WORLD_LIGHT_ENABLED = Setting.bool(NarutoConfig.WORLD_LIGHT_ENABLED, true, false);
    public static final Setting<Boolean> WORLD_AUDIO_ENABLED = Setting.bool(NarutoConfig.WORLD_AUDIO_ENABLED, true, false);
    public static final Setting<Float> WORLD_VOLUME = Setting.single(NarutoConfig.WORLD_VOLUME, 1.0F, false);
    public static final Setting<Integer> WORLD_AUDIO_MAX_DISTANCE = Setting.integer(NarutoConfig.WORLD_AUDIO_MAX_DISTANCE, 32, false);
    public static final Setting<Integer> WORLD_MAX_AUDIO = Setting.integer(NarutoConfig.WORLD_MAX_AUDIO, 2, false);

    public static final List<Setting<?>> ALL = List.of(
            MAX_VIDEO_WIDTH,
            MAX_VIDEO_HEIGHT,
            AUDIO_VOLUME,
            FFMPEG_DIR,
            YTDLP_DIR,
            SOURCES_DIR,
            BUFFER_SECONDS,
            SELECT_MODE,
            FIXED_NAME,
            MUTE_MUSIC,
            DEBUG_TIME_COSTS,
            DEBUG_TIME_COSTS_INTERVAL,
            SHIMMER,
            SHIMMER_SPEED,
            SHIMMER_SCALE,
            SHIMMER_STRENGTH,
            SHIMMER_FPS,
            FADABLE,
            COMPAT_MODE,
            TICKS_BEFORE_FADE,
            FADE_OUT_STEP,
            FADE_IN_STEP,
            WORLD_RENDER_MODE,
            WORLD_MAX_WIDTH,
            WORLD_MAX_HEIGHT,
            WORLD_BUFFER_SECONDS,
            WORLD_ACTIVE_DISTANCE,
            WORLD_MAX_ACTIVE,
            WORLD_CULL_INVISIBLE,
            WORLD_CULL_FACES,
            WORLD_LIGHT_ENABLED,
            WORLD_AUDIO_ENABLED,
            WORLD_VOLUME,
            WORLD_AUDIO_MAX_DISTANCE,
            WORLD_MAX_AUDIO
    );

    private NarutoSettings() {}
}
