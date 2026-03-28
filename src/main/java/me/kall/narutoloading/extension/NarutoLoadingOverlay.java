package me.kall.narutoloading.extension;

import com.mojang.blaze3d.platform.Window;
import me.kall.narutoloading.core.NarutoRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraftforge.client.loading.ForgeLoadingOverlay;
import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import net.minecraftforge.fml.loading.progress.ProgressMeter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

public class NarutoLoadingOverlay {
    private final IForgeLoadingOverlay forgeLoadingOverlay;

    public NarutoLoadingOverlay(ForgeLoadingOverlay forgeLoadingOverlay) {
        this.forgeLoadingOverlay = (IForgeLoadingOverlay) forgeLoadingOverlay;
    }

    public void render(final @NotNull GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        NarutoRenderer.getInstance().renderFrame();

        Minecraft minecraft = this.forgeLoadingOverlay.naruto$minecraft();
        ReloadInstance reload = this.forgeLoadingOverlay.naruto$reload();
        ProgressMeter progress = this.forgeLoadingOverlay.naruto$progress();
        long fadeOutStart = this.forgeLoadingOverlay.naruto$fadeOutStart();

        progress.setAbsolute(Mth.clamp((int) (reload.getActualProgress() * 100F), 0, 100));

        float fadeOutTimer = fadeOutStart > -1L ? (float) (Util.getMillis() - fadeOutStart) / 1000.0F : -1.0F;

        this.processOverlay(graphics, mouseX, mouseY, partialTick, minecraft, fadeOutTimer);

        if (fadeOutStart == -1L && reload.isDone()) this.finalize(minecraft, reload, progress);
    }

    private void processOverlay(GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick, Minecraft minecraft, float fadeOutTimer) {
        if (fadeOutTimer >= 1.0F) {
            Screen screen = minecraft.screen;
            if (screen != null) screen.render(graphics, mouseX, mouseY, partialTick);
        }

        DisplayWindow displayWindow = this.forgeLoadingOverlay.naruto$displayWindow();
        displayWindow.render(0xFF);

        if (fadeOutTimer >= 2.0F) {
            minecraft.setOverlay(null);
            displayWindow.close();
        }
    }

    private void finalize(Minecraft minecraft, ReloadInstance reload, @NotNull ProgressMeter progress) {
        progress.complete();
        this.forgeLoadingOverlay.naruto$setFadeOutStart(Util.getMillis());

        Consumer<Optional<Throwable>> onFinish = this.forgeLoadingOverlay.naruto$onFinish();

        try {
            reload.checkExceptions();
            onFinish.accept(Optional.empty());
        } catch (Throwable throwable) {
            onFinish.accept(Optional.of(throwable));
        }

        Screen screen = minecraft.screen;
        if (screen != null) {
            Window window = minecraft.getWindow();
            int width = window.getGuiScaledWidth();
            int height = window.getGuiScaledHeight();
            screen.init(minecraft, width, height);
        }
    }
}