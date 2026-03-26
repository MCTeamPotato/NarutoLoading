package me.kall.narutoloading.mixin.overlay;

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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderLoadingOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, @NotNull CallbackInfo ci) {
        ci.cancel();
        long millis = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = millis;
        }

        float fadeOutTimer = this.fadeOutStart > -1L ? (float)(millis - this.fadeOutStart) / 1000.0F : -1.0F;
        float fadeInTimer = this.fadeInStart > -1L ? (float)(millis - this.fadeInStart) / 500.0F : -1.0F;

        if (fadeOutTimer >= 1.0F) {
            if (this.minecraft.screen != null) {
                this.minecraft.screen.render(guiGraphics, 0, 0, partialTick);
            }
        } else if (this.fadeIn) {
            if (this.minecraft.screen != null && fadeInTimer < 1.0F) {
                this.minecraft.screen.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        int j1 = (int)(Math.min(guiGraphics.guiWidth() * 0.75, guiGraphics.guiHeight()) * 0.5);
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + this.reload.getActualProgress() * 0.050000012F, 0.0F, 1.0F);
        if (fadeOutTimer < 1.0F) {
            int guiWidth = guiGraphics.guiWidth();
            int scaledGuiHeight = (int)(guiGraphics.guiHeight() * 0.8325);
            this.drawProgressBar(guiGraphics, guiWidth / 2 - j1, scaledGuiHeight - 5, guiWidth / 2 + j1, scaledGuiHeight + 5, 1.0F - Mth.clamp(fadeOutTimer, 0.0F, 1.0F));
        }

        if (fadeOutTimer >= 2.0F) {
            this.minecraft.setOverlay(null);
        }

        if (this.fadeOutStart == -1L && this.reload.isDone() && (!this.fadeIn || fadeInTimer >= 2.0F)) {
            this.fadeOutStart = Util.getMillis();

            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable var23) {
                this.onFinish.accept(Optional.of(var23));
            }

            if (this.minecraft.screen != null) {
                this.minecraft.screen.init(this.minecraft, guiGraphics.guiWidth(), guiGraphics.guiHeight());
            }
        }
    }
}
