package me.kall.narutoloading.mixin;

import me.kall.narutoloading.NarutoLoading;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraftforge.client.loading.ForgeLoadingOverlay;
import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import net.minecraftforge.fml.loading.progress.ProgressMeter;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(value = ForgeLoadingOverlay.class, remap = false)
public abstract class MixinForgeLoadingOverlay extends LoadingOverlay {
    @Shadow private long fadeOutStart;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private ProgressMeter progress;
    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private DisplayWindow displayWindow;

    public MixinForgeLoadingOverlay(Minecraft minecraft, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish, boolean fadeIn) {
        super(minecraft, reload, onFinish, fadeIn);
    }

    /**
     * @author Kasualix
     * @reason NarutoLoading
     */
    @Overwrite
    public void render(final @NotNull GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        float fadeOutTimer = this.fadeOutStart > -1L ? (float)(Util.getMillis() - this.fadeOutStart) / 1000.0F : -1.0F;

        if (fadeOutTimer >= 2.0F) {
            this.minecraft.setOverlay(null);
            this.displayWindow.close();
        }

        if (this.fadeOutStart == -1L && this.reload.isDone()) {
            progress.complete();
            this.fadeOutStart = Util.getMillis();
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable throwable) {
                this.onFinish.accept(Optional.of(throwable));
            }

            if (this.minecraft.screen != null) {
                this.minecraft.screen.init(this.minecraft, this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
            }
        }

        NarutoLoading.RENDERER.renderFrame(graphics);
    }
}