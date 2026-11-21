package me.kall.narutoloading.impl;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.client.context.Graphics;
import me.kall.narutoloading.client.renderer.ImageFrameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class ClientUiRenderer extends ImageFrameRenderer {
    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "client_ui");

    @Override
    protected @NotNull ResourceLocation setLocation() {
        return LOCATION;
    }

    @Override
    public void render() {
        GuiGraphics graphics = Graphics.get();
        if (graphics == null || this.textureLocation == null) return;

        int w = graphics.guiWidth();
        int h = graphics.guiHeight();

        graphics.blit(this.textureLocation, 0, 0, 0, 0, w, h, w, h);
    }
}
