package me.kall.narutoloading.mixin.overlay;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.extension.IForgeLoadingOverlay;
import me.kall.narutoloading.extension.NarutoLoadingOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraftforge.client.loading.ForgeLoadingOverlay;
import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import net.minecraftforge.fml.loading.progress.ProgressMeter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(value = ForgeLoadingOverlay.class)
public class MixinForgeLoadingOverlay implements IForgeLoadingOverlay {
    @Shadow(remap = false) @Final private Minecraft minecraft;
    @Shadow(remap = false) @Final private ReloadInstance reload;
    @Shadow(remap = false) @Final private Consumer<Optional<Throwable>> onFinish;
    @Shadow(remap = false) @Final private DisplayWindow displayWindow;
    @Shadow(remap = false) @Final private ProgressMeter progress;
    @Shadow(remap = false) private long fadeOutStart;

    @Unique private NarutoLoadingOverlay naruto$loadingOverlay;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initLoadingOverlay(CallbackInfo ci) {
        this.naruto$loadingOverlay = new NarutoLoadingOverlay((ForgeLoadingOverlay) (Object) this);
    }

    @Override
    public Minecraft naruto$minecraft() {
        return this.minecraft;
    }

    @Override
    public ReloadInstance naruto$reload() {
        return this.reload;
    }

    @Override
    public Consumer<Optional<Throwable>> naruto$onFinish() {
        return this.onFinish;
    }

    @Override
    public DisplayWindow naruto$displayWindow() {
        return this.displayWindow;
    }

    @Override
    public ProgressMeter naruto$progress() {
        return this.progress;
    }

    @Override
    public long naruto$fadeOutStart() {
        return this.fadeOutStart;
    }

    @Override
    public void naruto$setFadeOutStart(long fadeOutStart) {
        this.fadeOutStart = fadeOutStart;
    }

    @WrapMethod(method = "render")
    private void renderForgeLoadingOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
        this.naruto$loadingOverlay.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
