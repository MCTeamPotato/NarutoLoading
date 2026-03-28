package me.kall.narutoloading.extension;

import com.mojang.blaze3d.platform.Window;
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
    private final ForgeLoadingOverlay forgeLoadingOverlay;

    public NarutoLoadingOverlay(ForgeLoadingOverlay forgeLoadingOverlay) {
        this.forgeLoadingOverlay = forgeLoadingOverlay;
    }

    public void render(final @NotNull GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        Minecraft minecraft = ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$minecraft();
        ReloadInstance reload = ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$reload();
        ProgressMeter progress = ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$progress();
        long fadeOutStart = ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$fadeOutStart();

        float fadeOutTimer = fadeOutStart > -1L ? (float) (Util.getMillis() - fadeOutStart) / 1000.0F : -1.0F;
        progress.setAbsolute(Mth.clamp((int) (reload.getActualProgress() * 100F), 0, 100));

        if (fadeOutTimer >= 1.0F) {
            Screen screen = minecraft.screen;
            if (screen != null) {
                screen.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        DisplayWindow displayWindow = ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$displayWindow();

        displayWindow.render(0xFF);

        if (fadeOutTimer >= 2.0F) {
            minecraft.setOverlay(null);
            displayWindow.close();
        }

        if (fadeOutStart == -1L && reload.isDone()) {
            progress.complete();
            ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$setFadeOutStart(Util.getMillis());

            Consumer<Optional<Throwable>> onFinish =((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$onFinish();

            try {
                reload.checkExceptions();
                onFinish.accept(Optional.empty());
            } catch (Throwable throwable) {
                onFinish.accept(Optional.of(throwable));
            }

            Screen screen = minecraft.screen;
            if (screen != null) {
                Window window = minecraft.getWindow();
                screen.init(minecraft, window.getGuiScaledWidth(), window.getGuiScaledHeight());
            }
        }
    }
}
