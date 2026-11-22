package me.kall.narutoloading.client.ui;

import icyllis.modernui.core.Context;
import me.kall.narutoui.mui.choice.MuiOption;
import me.kall.narutoui.mui.color.MuiColorScheme;
import me.kall.narutoui.mui.color.MuiColorSchemes;
import me.kall.narutoui.mui.theme.MuiTheme;
import me.kall.narutoui.mui.widget.MuiSchemeSwatch;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class NarutoTheme {
    static final String AUTO = "AUTO";

    private NarutoTheme() {}

    static @NotNull MuiTheme resolve(@Nullable String id, @NotNull Context context) {
        MuiColorScheme scheme = MuiColorSchemes.byId(id);
        return scheme == null
                ? MuiTheme.modern(context)
                : scheme.theme().toTheme(MuiTheme.DEFAULT_CORNER_RADIUS, MuiTheme.DEFAULT_SPACING);
    }

    static int @NotNull [] ramp(@Nullable String id) {
        MuiColorScheme scheme = MuiColorSchemes.byId(id);
        return scheme == null ? MuiColorSchemes.NARUTO.shimmerRamp() : scheme.shimmerRamp();
    }

    static @NotNull String normalized(@Nullable String id) {
        MuiColorScheme scheme = MuiColorSchemes.byId(id);
        return scheme == null ? AUTO : scheme.id();
    }

    static @NotNull List<MuiOption<String>> options() {
        List<MuiColorScheme> schemes = MuiColorSchemes.all();
        List<MuiOption<String>> options = new ArrayList<>(schemes.size() + 1);

        options.add(MuiOption.of(AUTO, "narutoloading.general.theme.auto")
                .preview((context, theme) -> MuiSchemeSwatch.modern(context)));

        for (MuiColorScheme scheme : schemes) {
            options.add(MuiOption.of(scheme.id(), name(scheme))
                    .preview((context, theme) -> MuiSchemeSwatch.of(context, scheme.theme())));
        }

        return options;
    }

    private static @NotNull String name(@NotNull MuiColorScheme scheme) {
        String key = scheme.nameKey();
        String translated = I18n.get(key);
        return translated.equals(key) ? scheme.id() : translated;
    }
}
