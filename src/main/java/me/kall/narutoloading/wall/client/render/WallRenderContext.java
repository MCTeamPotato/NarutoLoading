package me.kall.narutoloading.wall.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;

public final class WallRenderContext {
    private static @Nullable PoseStack poseStack;
    private static @Nullable Camera camera;
    private static @Nullable MultiBufferSource.BufferSource bufferSource;

    private WallRenderContext() {}

    public static void set(@Nullable PoseStack poseStack, @Nullable Camera camera, @Nullable MultiBufferSource.BufferSource bufferSource) {
        WallRenderContext.poseStack = poseStack;
        WallRenderContext.camera = camera;
        WallRenderContext.bufferSource = bufferSource;
    }

    public static @Nullable PoseStack poseStack() {
        return poseStack;
    }

    public static @Nullable Camera camera() {
        return camera;
    }

    public static @Nullable MultiBufferSource.BufferSource bufferSource() {
        return bufferSource;
    }

    public static void clear() {
        set(null, null, null);
    }
}
