package me.kall.narutoloading.client.ui;

import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.config.NarutoSettings;
import me.kall.narutoloading.wall.WallRenderMode;
import me.kall.narutoloading.wall.client.light.WallLightHook;
import me.kall.narutoui.mui.choice.MuiOption;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import me.kall.narutoui.mui.shimmer.MuiShimmer;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Locale;

final class ConfigPage implements SettingsPageBuilder {
    private final NarutoScreen screen;

    ConfigPage(@NotNull NarutoScreen screen) {
        this.screen = screen;
    }

    @Override
    public void build(@NotNull MuiSettingsPage page) {
        page.shimmerAll();

        page.section("narutoloading.config.section.video", section -> section
                        .number("narutoloading.config.width", "narutoloading.config.width.desc", 16, 7680, () -> this.screen.draft().get(NarutoSettings.MAX_VIDEO_WIDTH), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.MAX_VIDEO_WIDTH, value)))
                        .number("narutoloading.config.height", "narutoloading.config.height.desc", 16, 4320, () -> this.screen.draft().get(NarutoSettings.MAX_VIDEO_HEIGHT), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.MAX_VIDEO_HEIGHT, value)))
                        .decimal("narutoloading.config.buffer", "narutoloading.config.buffer.desc", 0.0D, 5.0D, () -> this.screen.draft().get(NarutoSettings.BUFFER_SECONDS), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.BUFFER_SECONDS, value)))
                        .toggle("narutoloading.config.compatMode", "narutoloading.config.compatMode.desc", () -> this.screen.draft().get(NarutoSettings.COMPAT_MODE), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.COMPAT_MODE, value)))
                        .dropdown("narutoloading.config.worldMode", "narutoloading.config.worldMode.desc", worldModes(), this.screen.draft().get(NarutoSettings.WORLD_RENDER_MODE), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_RENDER_MODE, value))))
                .section("narutoloading.config.section.world", section -> section
                        .number("narutoloading.config.worldWidth", "narutoloading.config.worldWidth.desc", 16, 3840, () -> this.screen.draft().get(NarutoSettings.WORLD_MAX_WIDTH), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_MAX_WIDTH, value)))
                        .number("narutoloading.config.worldHeight", "narutoloading.config.worldHeight.desc", 16, 2160, () -> this.screen.draft().get(NarutoSettings.WORLD_MAX_HEIGHT), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_MAX_HEIGHT, value)))
                        .decimal("narutoloading.config.worldBuffer", "narutoloading.config.worldBuffer.desc", 0.0D, 5.0D, () -> this.screen.draft().get(NarutoSettings.WORLD_BUFFER_SECONDS), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_BUFFER_SECONDS, value)))
                        .number("narutoloading.config.worldDistance", "narutoloading.config.worldDistance.desc", 4, 256, () -> this.screen.draft().get(NarutoSettings.WORLD_ACTIVE_DISTANCE), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_ACTIVE_DISTANCE, value)))
                        .number("narutoloading.config.worldMaxActive", "narutoloading.config.worldMaxActive.desc", 1, 32, () -> this.screen.draft().get(NarutoSettings.WORLD_MAX_ACTIVE), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_MAX_ACTIVE, value)))
                        .toggle("narutoloading.config.worldCull", "narutoloading.config.worldCull.desc", () -> this.screen.draft().get(NarutoSettings.WORLD_CULL_INVISIBLE), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_CULL_INVISIBLE, value)))
                        .toggle("narutoloading.config.worldCullFaces", "narutoloading.config.worldCullFaces.desc", () -> this.screen.draft().get(NarutoSettings.WORLD_CULL_FACES), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_CULL_FACES, value)))
                        .toggle("narutoloading.config.worldLight", "narutoloading.config.worldLight.desc", () -> this.screen.draft().get(NarutoSettings.WORLD_LIGHT_ENABLED), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_LIGHT_ENABLED, value)))
                        .enabledWhen(WallLightHook::active)
                        .toggle("narutoloading.config.worldAudio", "narutoloading.config.worldAudio.desc", () -> this.screen.draft().get(NarutoSettings.WORLD_AUDIO_ENABLED), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_AUDIO_ENABLED, value)))
                        .slider("narutoloading.config.worldVolume", "narutoloading.config.worldVolume.desc", 0.0D, 2.0D, 0.05D, () -> (double) this.screen.draft().get(NarutoSettings.WORLD_VOLUME), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_VOLUME, (float) value)), value -> String.format(Locale.ROOT, "%.2f", value))
                        .number("narutoloading.config.worldAudioDistance", "narutoloading.config.worldAudioDistance.desc", 4, 128, () -> this.screen.draft().get(NarutoSettings.WORLD_AUDIO_MAX_DISTANCE), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_AUDIO_MAX_DISTANCE, value)))
                        .number("narutoloading.config.worldMaxAudio", "narutoloading.config.worldMaxAudio.desc", 1, 8, () -> this.screen.draft().get(NarutoSettings.WORLD_MAX_AUDIO), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.WORLD_MAX_AUDIO, value))))
                .section("narutoloading.config.section.appearance", section -> section
                        .toggle("narutoloading.config.shimmer", "narutoloading.config.shimmer.desc", () -> this.screen.draft().get(NarutoSettings.SHIMMER),
                                value -> {
                                    this.screen.edit(() -> this.screen.draft().set(NarutoSettings.SHIMMER, value));
                                    MuiShimmer.refresh();
                                })
                        .slider("narutoloading.config.shimmerSpeed", "narutoloading.config.shimmerSpeed.desc", 0.2D, 3.0D, 0.1D, () -> this.screen.draft().get(NarutoSettings.SHIMMER_SPEED), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.SHIMMER_SPEED, value)), value -> String.format(Locale.ROOT, "%.1f", value))
                        .enabledWhen(() -> this.screen.draft().get(NarutoSettings.SHIMMER))
                        .slider("narutoloading.config.shimmerScale", "narutoloading.config.shimmerScale.desc", 0.2D, 3.0D, 0.1D, () -> this.screen.draft().get(NarutoSettings.SHIMMER_SCALE), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.SHIMMER_SCALE, value)), value -> String.format(Locale.ROOT, "%.1f", value))
                        .enabledWhen(() -> this.screen.draft().get(NarutoSettings.SHIMMER))
                        .slider("narutoloading.config.shimmerStrength", "narutoloading.config.shimmerStrength.desc", 0.0D, 1.0D, 0.05D, () -> this.screen.draft().get(NarutoSettings.SHIMMER_STRENGTH), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.SHIMMER_STRENGTH, value)), value -> String.format(Locale.ROOT, "%.2f", value))
                        .enabledWhen(() -> this.screen.draft().get(NarutoSettings.SHIMMER))
                        .slider("narutoloading.config.shimmerFps", "narutoloading.config.shimmerFps.desc", 5, 120, () -> this.screen.draft().get(NarutoSettings.SHIMMER_FPS), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.SHIMMER_FPS, value)), Integer::toString)
                        .enabledWhen(() -> this.screen.draft().get(NarutoSettings.SHIMMER))
                        .toggle("narutoloading.config.fade", "narutoloading.config.fade.desc", () -> this.screen.draft().get(NarutoSettings.FADABLE), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.FADABLE, value)))
                        .number("narutoloading.config.fadeTicks", "narutoloading.config.fadeTicks.desc", 0, 1200, () -> this.screen.draft().get(NarutoSettings.TICKS_BEFORE_FADE), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.TICKS_BEFORE_FADE, value)))
                        .number("narutoloading.config.fadeOutStep", "narutoloading.config.fadeOutStep.desc", 0, 100, () -> this.screen.draft().get(NarutoSettings.FADE_OUT_STEP), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.FADE_OUT_STEP, value)))
                        .number("narutoloading.config.fadeInStep", "narutoloading.config.fadeInStep.desc", 0, 100, () -> this.screen.draft().get(NarutoSettings.FADE_IN_STEP), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.FADE_IN_STEP, value)))
                        .enabledWhen(() -> this.screen.draft().get(NarutoSettings.FADABLE)))
                .section("narutoloading.config.section.sources", section -> section
                        .textField("narutoloading.config.sourcesDir", "narutoloading.config.sourcesDir.desc", this.screen.draft().get(NarutoSettings.SOURCES_DIR), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.SOURCES_DIR, value)), field -> field.onChange(value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.SOURCES_DIR, value))))
                        .dropdown("narutoloading.config.mode", "narutoloading.config.mode.desc", modes(), this.screen.draft().get(NarutoSettings.SELECT_MODE), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.SELECT_MODE, value)))
                        .textField("narutoloading.config.fixedName", "narutoloading.config.fixedName.desc", this.screen.draft().get(NarutoSettings.FIXED_NAME), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.FIXED_NAME, value)), field -> field.onChange(value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.FIXED_NAME, value))))
                        .enabledWhen(() -> this.screen.draft().get(NarutoSettings.SELECT_MODE) == MediaRepository.Mode.FIXED))
                .section("narutoloading.config.section.debug", section -> section
                        .toggle("narutoloading.config.debug", "narutoloading.config.debug.desc", () -> this.screen.draft().get(NarutoSettings.DEBUG_TIME_COSTS), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.DEBUG_TIME_COSTS, value)))
                        .number("narutoloading.config.debugInterval", "narutoloading.config.debugInterval.desc", 1, 600, () -> this.screen.draft().get(NarutoSettings.DEBUG_TIME_COSTS_INTERVAL), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.DEBUG_TIME_COSTS_INTERVAL, value)))
                        .enabledWhen(() -> this.screen.draft().get(NarutoSettings.DEBUG_TIME_COSTS)));
    }

    @Contract(" -> new")
    private static @NotNull @Unmodifiable List<MuiOption<MediaRepository.Mode>> modes() {
        return List.of(MuiOption.of(MediaRepository.Mode.RANDOM, I18n.get("narutoloading.config.mode.random")), MuiOption.of(MediaRepository.Mode.SEQUENTIAL, I18n.get("narutoloading.config.mode.sequential")), MuiOption.of(MediaRepository.Mode.FIXED, I18n.get("narutoloading.config.mode.fixed")));
    }

    @Contract(" -> new")
    private static @NotNull @Unmodifiable List<MuiOption<WallRenderMode>> worldModes() {
        return List.of(MuiOption.of(WallRenderMode.AUTO, I18n.get("narutoloading.config.worldMode.auto")), MuiOption.of(WallRenderMode.BUFFER, I18n.get("narutoloading.config.worldMode.buffer")), MuiOption.of(WallRenderMode.IMAGE, I18n.get("narutoloading.config.worldMode.image")));
    }
}
