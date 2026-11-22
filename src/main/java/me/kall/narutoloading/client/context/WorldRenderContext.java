package me.kall.narutoloading.client.context;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;

public final class WorldRenderContext {
    private static @Nullable PoseStack poseStack;
    private static @Nullable Camera camera;
    private static @Nullable MultiBufferSource.BufferSource bufferSource;

    private WorldRenderContext() {}

    public static void set(@Nullable PoseStack poseStack, @Nullable Camera camera, @Nullable MultiBufferSource.BufferSource bufferSource) {
        WorldRenderContext.poseStack = poseStack;
        WorldRenderContext.camera = camera;
        WorldRenderContext.bufferSource = bufferSource;
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
