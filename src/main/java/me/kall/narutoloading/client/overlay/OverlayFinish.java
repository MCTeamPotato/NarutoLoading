package me.kall.narutoloading.client.overlay;

import net.minecraft.server.packs.resources.ReloadInstance;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

public final class OverlayFinish {
    private OverlayFinish() {}

    public static void finish(@NotNull ReloadInstance reload, @NotNull Consumer<Optional<Throwable>> onFinish) {
        try {
            reload.checkExceptions();
            onFinish.accept(Optional.empty());
        } catch (Throwable throwable) {
            onFinish.accept(Optional.of(throwable));
        }
    }
}
