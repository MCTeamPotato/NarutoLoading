package me.kall.narutoloading.client.ui;

import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.config.Setting;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Live copy of the client settings used by the screen.
 * <p>
 * Settings are written to disk as soon as they change, so this type only keeps the values that
 * widgets read while the screen is open. It used to work as a draft with a separate saved map;
 * the immediate-save model removed that distinction.
 */
final class NarutoSettingsState {
    private final Map<Setting<?>, Object> values;

    private NarutoSettingsState(@NotNull Map<Setting<?>, Object> values) {
        this.values = new LinkedHashMap<>(values);
    }

    static @NotNull NarutoSettingsState load() {
        return new NarutoSettingsState(NarutoConfig.snapshot());
    }

    @SuppressWarnings("unchecked")
    <T> T get(@NotNull Setting<T> setting) {
        return (T) this.values.get(setting);
    }

    <T> void set(@NotNull Setting<T> setting, T value) {
        this.values.put(setting, value);
    }
}
