package me.kall.narutoloading.client.ui;

import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.config.NarutoSettings;
import me.kall.narutoloading.config.Setting;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class NarutoDraft {
    private final Map<Setting<?>, Object> saved;
    private final Map<Setting<?>, Object> values;

    private NarutoDraft(@NotNull Map<Setting<?>, Object> saved) {
        this.saved = new LinkedHashMap<>(saved);
        this.values = new LinkedHashMap<>(saved);
    }

    static @NotNull NarutoDraft load() {
        return new NarutoDraft(NarutoConfig.snapshot());
    }

    @SuppressWarnings("unchecked")
    <T> T get(@NotNull Setting<T> setting) {
        return (T) this.values.get(setting);
    }

    <T> void set(@NotNull Setting<T> setting, T value) {
        this.values.put(setting, value);
    }

    boolean dirty() {
        for (Setting<?> setting : NarutoSettings.ALL) {
            if (this.changed(setting)) return true;
        }

        return false;
    }

    boolean playbackChanged() {
        for (Setting<?> setting : NarutoSettings.ALL) {
            if (!setting.playback()) continue;
            if (this.changed(setting)) return true;
        }

        return false;
    }

    void write() {
        Map<Setting<?>, Object> changed = new LinkedHashMap<>();

        for (Setting<?> setting : NarutoSettings.ALL) {
            if (this.changed(setting)) changed.put(setting, this.values.get(setting));
        }

        NarutoConfig.writeAll(changed);
        NarutoConfig.save();

        this.saved.clear();
        this.saved.putAll(this.values);
    }

    private boolean changed(@NotNull Setting<?> setting) {
        return !Objects.equals(this.values.get(setting), this.saved.get(setting));
    }
}
