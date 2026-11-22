package me.kall.narutoloading.client.ui;

import me.kall.narutoui.mui.screen.MuiSettingsPage;
import org.jetbrains.annotations.NotNull;

interface SettingsPageBuilder {
    void build(@NotNull MuiSettingsPage page);
}
