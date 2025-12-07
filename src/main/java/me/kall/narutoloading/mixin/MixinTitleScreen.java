package me.kall.narutoloading.mixin;

import me.kall.narutoloading.NarutoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    @Unique
    private final Button narutoLoading$replayButton = narutoLoading$buildButton();

    @Unique
    private static @NotNull Button narutoLoading$buildButton() {
        int width = 200;
        int height = 20;

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        int x = (screenWidth - width) / 2;
        int y = screenHeight - height - 10;

        return Button.builder(Component.translatable("button.narutoloading.replay"), button -> {
            NarutoRenderer.shutdown();
            NarutoRenderer.setup();
        }).bounds(x, y, width, height).build();
    }

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V")) private void disable(PanoramaRenderer instance, float f, float deltaT) {}
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V")) private void disable(GuiGraphics instance, ResourceLocation atlasLocation, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {}

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableBlend()V", remap = false, shift = At.Shift.AFTER))
    private void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        NarutoRenderer.renderFrame(graphics);
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void remove(CallbackInfo ci) {
        NarutoRenderer.shutdown();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void replayButton(CallbackInfo ci) {
        this.addRenderableWidget(narutoLoading$replayButton);
    }
}