package me.kall.narutoloading.world;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum WorldRenderMode {
    AUTO, BUFFER, IMAGE;

    public static @NotNull WorldRenderMode parse(@Nullable String value) {
        if (value == null) return AUTO;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return AUTO;
        }
    }
}
