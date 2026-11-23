package me.kall.narutoloading.client.ui;

import icyllis.modernui.view.View;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collapses rapid setting changes into one save, posted from the page's content view so the
 * save lands on the UI thread after the user stops dragging.
 */
final class SaveDebouncer {
    private static final long DELAY = 400L;

    private @Nullable View anchor;
    private int generation;

    void anchor(@Nullable View view) {
        this.anchor = view;
    }

    void schedule(@NotNull Runnable save) {
        int current = ++this.generation;

        View view = this.anchor;
        if (view == null) {
            save.run();
            return;
        }

        view.postDelayed(() -> {
            if (current == this.generation) save.run();
        }, DELAY);
    }
}
