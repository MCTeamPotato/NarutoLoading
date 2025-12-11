package me.kall.narutoloading.mixin.impl.overlay;

import me.kall.narutoloading.NarutoLoading;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {
    @Shadow @Final private boolean fadeIn;
    @Shadow private long fadeInStart;
    @Shadow private long fadeOutStart;
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;

    /**
     * @author Kasualix
     * @reason NarutoLoading
     */
    @Overwrite
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long k = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = k;
        }

        float f = this.fadeOutStart > -1L ? (float)(k - this.fadeOutStart) / 1000.0F : -1.0F;
        float f1 = this.fadeInStart > -1L ? (float)(k - this.fadeInStart) / 500.0F : -1.0F;

        if (f >= 2.0F) this.minecraft.setOverlay(null);


        if (this.fadeOutStart == -1L && this.reload.isDone() && (!this.fadeIn || f1 >= 2.0F)) {
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

        if (this.minecraft.getOverlay() instanceof LoadingOverlay) {
            NarutoLoading.RENDERER.renderFrame(guiGraphics);
        }
    }
}
