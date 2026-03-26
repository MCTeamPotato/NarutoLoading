package me.kall.narutoloading.extension;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraftforge.client.loading.ForgeLoadingOverlay;
import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import net.minecraftforge.fml.loading.progress.ProgressMeter;

import java.util.Optional;
import java.util.function.Consumer;

public interface IForgeLoadingOverlay {
    Minecraft naruto$minecraft();

    ReloadInstance naruto$reload();

    Consumer<Optional<Throwable>> naruto$onFinish();

    DisplayWindow naruto$displayWindow();

    ProgressMeter naruto$progress();

    long naruto$fadeOutStart();

    void naruto$setFadeOutStart(long fadeOutStart);

    static Minecraft minecraft(ForgeLoadingOverlay forgeLoadingOverlay) {
        return ((IForgeLoadingOverlay)forgeLoadingOverlay).naruto$minecraft();
    }

    static ReloadInstance reload(ForgeLoadingOverlay forgeLoadingOverlay) {
        return ((IForgeLoadingOverlay)forgeLoadingOverlay).naruto$reload();
    }

    static Consumer<Optional<Throwable>> onFinish(ForgeLoadingOverlay forgeLoadingOverlay) {
        return ((IForgeLoadingOverlay)forgeLoadingOverlay).naruto$onFinish();
    }

    static DisplayWindow displayWindow(ForgeLoadingOverlay forgeLoadingOverlay) {
        return ((IForgeLoadingOverlay)forgeLoadingOverlay).naruto$displayWindow();
    }

    static ProgressMeter progress(ForgeLoadingOverlay forgeLoadingOverlay) {
        return ((IForgeLoadingOverlay)forgeLoadingOverlay).naruto$progress();
    }

    static long fadeOutStart(ForgeLoadingOverlay forgeLoadingOverlay) {
        return ((IForgeLoadingOverlay)forgeLoadingOverlay).naruto$fadeOutStart();
    }

    static void setFadeOutStart(ForgeLoadingOverlay forgeLoadingOverlay, long fadeOutStart) {
        ((IForgeLoadingOverlay)forgeLoadingOverlay).naruto$setFadeOutStart(fadeOutStart);
    }
}
