package me.kall.narutoloading.extension;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadInstance;
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
}
