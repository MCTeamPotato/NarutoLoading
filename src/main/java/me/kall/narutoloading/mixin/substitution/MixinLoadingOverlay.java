package me.kall.narutoloading.mixin.substitution;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.client.overlay.OverlayFinish;
import me.kall.narutoloading.client.overlay.Substitution;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;
    @Shadow @Final private ReloadInstance reload;
    @Shadow private long fadeOutStart;
    @Shadow @Final private boolean fadeIn;
    @Shadow private float currentProgress;
    @Shadow private long fadeInStart;

    @Shadow protected abstract void drawProgressBar(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, float partialTick);

    @WrapMethod(method = "render")
    private void substitute(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
        if (Substitution.checkAndRun()) {
            Minecraft minecraft = this.minecraft;
            Consumer<Optional<Throwable>> onFinish = this.onFinish;
            ReloadInstance reload = this.reload;

            boolean fadeIn = this.fadeIn;

            long millis = Util.getMillis();
            if (fadeIn && this.fadeInStart == -1L) this.fadeInStart = millis;

            long fadeOutStart = this.fadeOutStart;
            long fadeInStart = this.fadeInStart;

            float fadeOutTimer = fadeOutStart > -1L ? (float)(millis - fadeOutStart) / 1000.0F : -1.0F;
            float fadeInTimer = fadeInStart > -1L ? (float)(millis - fadeInStart) / 500.0F : -1.0F;

            this.override$processOverlay(fadeOutTimer, fadeInTimer, guiGraphics, mouseX, mouseY, partialTick, minecraft, fadeIn, reload);

            if (fadeOutStart == -1L && reload.isDone() && (!fadeIn || fadeInTimer >= 2.0F)) this.override$finalize(guiGraphics, reload, onFinish, minecraft);
        } else {
            original.call(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Unique
    private void override$processOverlay(float fadeOutTimer, float fadeInTimer, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, Minecraft minecraft, boolean fadeIn, ReloadInstance reload) {
        if (fadeOutTimer >= 1.0F) {
            if (minecraft.screen != null) {
                minecraft.screen.render(guiGraphics, 0, 0, partialTick);
            }
        } else if (fadeIn) {
            if (minecraft.screen != null && fadeInTimer < 1.0F) {
                minecraft.screen.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + reload.getActualProgress() * 0.050000012F, 0.0F, 1.0F);
        if (fadeOutTimer < 1.0F) {
            int guiWidth = guiGraphics.guiWidth();
            int guiHeight = guiGraphics.guiHeight();

            int scaledGuiHeight = (int)(0.8325 * (double) guiHeight);
            int scaledBarHeight = (int)(Math.min(guiWidth * 0.75, guiHeight) * 0.5);

            int minX = guiWidth / 2 - scaledBarHeight;
            int minY = scaledGuiHeight - 5;
            int maxX = guiWidth / 2 + scaledBarHeight;
            int maxY = scaledGuiHeight + 5;

            float fadeTick = 1.0F - Mth.clamp(fadeOutTimer, 0.0F, 1.0F);

            this.drawProgressBar(guiGraphics, minX, minY, maxX, maxY, fadeTick);
        }

        if (fadeOutTimer >= 2.0F) minecraft.setOverlay(null);
    }

    @Unique
    private void override$finalize(GuiGraphics guiGraphics, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish, @NotNull Minecraft minecraft) {
        this.fadeOutStart = Util.getMillis();

        OverlayFinish.finish(reload, onFinish);

        if (minecraft.screen != null) {
            int guiWidth = guiGraphics.guiWidth();
            int guiHeight = guiGraphics.guiHeight();

            minecraft.screen.init(minecraft, guiWidth, guiHeight);
        }
    }
}