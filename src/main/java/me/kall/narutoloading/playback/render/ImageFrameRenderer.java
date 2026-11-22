package me.kall.narutoloading.playback.render;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutoloading.client.fade.Fadable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public abstract class ImageFrameRenderer implements FrameRenderer<NativeImage> {
    protected @Nullable DynamicTexture dynamicTexture;
    protected @Nullable ResourceLocation textureLocation;

    @Override
    public void setup(int width, int height, double seekTo, boolean ready) {
        this.dynamicTexture = new DynamicTexture(width, height, false);
        this.textureLocation = this.setLocation();

        Minecraft.getInstance().getTextureManager().register(this.textureLocation, this.dynamicTexture);
        Fadable.setUnfadable(this.textureLocation, true);

        if (ready) return;

        try (NativeImage loading = initLoading(width, height)) {
            this.dynamicTexture.setPixels(loading);
            this.dynamicTexture.upload();
        }
    }

    protected abstract @NotNull ResourceLocation setLocation();

    private static @NotNull NativeImage initLoading(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Loading.genImage(width, height, image);

        NativeImage nativeImage = new NativeImage(width, height, false);
        MemoryUtil.memIntBuffer(nativeImage.pixels, width * height).put(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
        return nativeImage;
    }

    @Override
    public void update(int width, int height, @Nullable NativeImage frame) {
        if (frame == null || this.dynamicTexture == null) return;
        this.dynamicTexture.setPixels(frame);
        this.dynamicTexture.upload();
    }

    @Override
    public void shutdown() {
        if (this.textureLocation != null) Minecraft.getInstance().getTextureManager().release(this.textureLocation);
        if (this.dynamicTexture != null) this.dynamicTexture.close();

        this.dynamicTexture = null;
        this.textureLocation = null;
    }
}
