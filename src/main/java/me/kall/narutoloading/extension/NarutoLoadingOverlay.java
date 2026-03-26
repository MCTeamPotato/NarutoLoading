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
        float fadeOutTimer = this.fadeOutStart() > -1L ? (float) (Util.getMillis() - this.fadeOutStart()) / 1000.0F : -1.0F;
        this.progress().setAbsolute(Mth.clamp((int) (this.reload().getActualProgress() * 100F), 0, 100));

        if (fadeOutTimer >= 1.0F) {
            Screen screen = this.minecraft().screen;
            if (screen != null) {
                screen.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        this.displayWindow().render(0xFF);

        if (fadeOutTimer >= 2.0F) {
            this.minecraft().setOverlay(null);
            this.displayWindow().close();
        }

        if (this.fadeOutStart() == -1L && this.reload().isDone()) {
            this.progress().complete();
            this.setFadeOutStart(Util.getMillis());

            try {
                this.reload().checkExceptions();
                this.onFinish().accept(Optional.empty());
            } catch (Throwable throwable) {
                this.onFinish().accept(Optional.of(throwable));
            }

            Screen screen = this.minecraft().screen;
            if (screen != null) {
                Window window = this.minecraft().getWindow();
                screen.init(this.minecraft(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
            }
        }
    }

    public Minecraft minecraft() {
        return ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$minecraft();
    }

    public ReloadInstance reload() {
        return ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$reload();
    }

    public Consumer<Optional<Throwable>> onFinish() {
        return ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$onFinish();
    }

    public DisplayWindow displayWindow() {
        return ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$displayWindow();
    }

    public ProgressMeter progress() {
        return ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$progress();
    }

    public long fadeOutStart() {
        return ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$fadeOutStart();
    }

    public void setFadeOutStart(long fadeOutStart) {
        ((IForgeLoadingOverlay)this.forgeLoadingOverlay).naruto$setFadeOutStart(fadeOutStart);
    }
}
