package me.kall.narutoloading.client.ui;

import me.kall.narutoloading.config.NarutoSettings;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import org.jetbrains.annotations.NotNull;

/**
 * Loading-screen player (ClientUiTV) settings. Nothing in this page affects the in-world walls.
 */
final class LoadingPage implements SettingsPageBuilder {
    private final NarutoScreen screen;

    LoadingPage(@NotNull NarutoScreen screen) {
        this.screen = screen;
    }

    @Override
    public void build(@NotNull MuiSettingsPage page) {
        NarutoSettingsState state = this.screen.settings();

        page.shimmerAll()
                .section("narutoloading.loading.section.video", section -> section
                        .number("narutoloading.loading.width", "narutoloading.loading.width.desc", 16, 7680, () -> state.get(NarutoSettings.MAX_VIDEO_WIDTH), value -> this.screen.set(NarutoSettings.MAX_VIDEO_WIDTH, value))
                        .number("narutoloading.loading.height", "narutoloading.loading.height.desc", 16, 4320, () -> state.get(NarutoSettings.MAX_VIDEO_HEIGHT), value -> this.screen.set(NarutoSettings.MAX_VIDEO_HEIGHT, value))
                        .decimal("narutoloading.loading.buffer", "narutoloading.loading.buffer.desc", 0.0D, 5.0D, () -> state.get(NarutoSettings.BUFFER_SECONDS), value -> this.screen.set(NarutoSettings.BUFFER_SECONDS, value)))
                .section("narutoloading.loading.section.sound", section -> section
                        .slider("narutoloading.loading.volume", "narutoloading.loading.volume.desc", 0, 100, () -> Math.round(state.get(NarutoSettings.AUDIO_VOLUME) * 100.0F), value -> this.screen.set(NarutoSettings.AUDIO_VOLUME, value / 100.0F), value -> value + "%")
                        .toggle("narutoloading.loading.mute", "narutoloading.loading.mute.desc", () -> state.get(NarutoSettings.MUTE_MUSIC), value -> this.screen.set(NarutoSettings.MUTE_MUSIC, value)))
                .section("narutoloading.loading.section.fade", section -> section
                        .toggle("narutoloading.loading.fade", "narutoloading.loading.fade.desc", () -> state.get(NarutoSettings.FADABLE), value -> this.screen.set(NarutoSettings.FADABLE, value))
                        .number("narutoloading.loading.fadeTicks", "narutoloading.loading.fadeTicks.desc", 0, 1200, () -> state.get(NarutoSettings.TICKS_BEFORE_FADE), value -> this.screen.set(NarutoSettings.TICKS_BEFORE_FADE, value))
                        .enabledWhen(() -> state.get(NarutoSettings.FADABLE))
                        .number("narutoloading.loading.fadeOut", "narutoloading.loading.fadeOut.desc", 0, 100, () -> state.get(NarutoSettings.FADE_OUT_STEP), value -> this.screen.set(NarutoSettings.FADE_OUT_STEP, value))
                        .enabledWhen(() -> state.get(NarutoSettings.FADABLE))
                        .number("narutoloading.loading.fadeIn", "narutoloading.loading.fadeIn.desc", 0, 100, () -> state.get(NarutoSettings.FADE_IN_STEP), value -> this.screen.set(NarutoSettings.FADE_IN_STEP, value))
                        .enabledWhen(() -> state.get(NarutoSettings.FADABLE))
                        .note("narutoloading.loading.fade.note", "narutoloading.loading.fade.note.desc"));
    }
}
