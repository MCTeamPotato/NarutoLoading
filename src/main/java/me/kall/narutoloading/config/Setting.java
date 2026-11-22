package me.kall.narutoloading.config;

import me.kall.narutoloading.config.api.Config;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Function;

public final class Setting<T> {
    private final String key;
    private final T defaultValue;
    private final Function<String, T> parser;
    private final Function<T, String> serializer;
    private final boolean playback;

    private Setting(@NotNull String key, @NotNull T defaultValue, @NotNull Function<String, T> parser, @NotNull Function<T, String> serializer, boolean playback) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.parser = parser;
        this.serializer = serializer;
        this.playback = playback;
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull Setting<Integer> integer(@NotNull String key, int defaultValue, boolean playback) {
        return new Setting<>(key, defaultValue, Setting::parseInteger, String::valueOf, playback);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull Setting<Double> decimal(@NotNull String key, double defaultValue, boolean playback) {
        return new Setting<>(key, defaultValue, Setting::parseDouble, String::valueOf, playback);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull Setting<Float> single(@NotNull String key, float defaultValue, boolean playback) {
        return new Setting<>(key, defaultValue, Setting::parseFloat, String::valueOf, playback);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull Setting<Boolean> bool(@NotNull String key, boolean defaultValue, boolean playback) {
        return new Setting<>(key, defaultValue, Setting::parseBoolean, String::valueOf, playback);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull Setting<String> text(@NotNull String key, @NotNull String defaultValue, boolean playback) {
        return new Setting<>(key, defaultValue, Function.identity(), Function.identity(), playback);
    }

    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    public static <T> @NotNull Setting<T> mapped(@NotNull String key, @NotNull T defaultValue, @NotNull Function<String, T> parser, @NotNull Function<T, String> serializer, boolean playback) {
        return new Setting<>(key, defaultValue, parser, serializer, playback);
    }

    public @NotNull String key() {
        return this.key;
    }

    public @NotNull T defaultValue() {
        return this.defaultValue;
    }

    public boolean playback() {
        return this.playback;
    }

    public @NotNull String encodedDefault() {
        return this.serializer.apply(this.defaultValue);
    }

    public @NotNull T read(@NotNull Config config) {
        String value = config.getString(this.key);
        if (value == null) return this.defaultValue;

        T parsed = this.parser.apply(value);
        return parsed == null ? this.defaultValue : parsed;
    }

    public void write(@NotNull Config config, @NotNull T value) {
        config.set(this.key, this.serializer.apply(value));
    }

    private static @Nullable Integer parseInteger(@NotNull String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static @Nullable Double parseDouble(@NotNull String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static @Nullable Float parseFloat(@NotNull String value) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static @Nullable Boolean parseBoolean(@NotNull String value) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);

        if (trimmed.equals("true") || trimmed.equals("yes") || trimmed.equals("on") || trimmed.equals("1")) return Boolean.TRUE;
        if (trimmed.equals("false") || trimmed.equals("no") || trimmed.equals("off") || trimmed.equals("0")) return Boolean.FALSE;

        return null;
    }
}
