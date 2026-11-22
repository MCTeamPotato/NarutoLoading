package me.kall.narutoloading.bootstrap;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.jetbrains.annotations.NotNull;

public final class Integration {
    public static final boolean MODERN_UI = modLoaded("modernui");
    public static final boolean NARUTO_UI = modLoaded("narutoui");
    public static final boolean EMBEDDIUM = modLoaded("embeddium");
    public static final boolean DUPLICATIONLESS = modLoaded("duplicationless");

    private Integration() {}

    public static boolean modLoaded(@NotNull String modId) {
        LoadingModList modList = FMLLoader.getLoadingModList();
        return modList != null && modList.getModFileById(modId) != null;
    }
}
