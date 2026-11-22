package me.kall.narutoloading.wall.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutoloading.wall.client.WallManager;
import me.kall.narutoloading.wall.client.WallTV;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class WallRenderer {
    private WallRenderer() {}

    public static void render(@NotNull PoseStack poseStack, @NotNull Camera camera, @NotNull Frustum frustum) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        List<WallTV<?>> walls = WallManager.getIn(level.dimension().location());
        if (walls.isEmpty()) return;

        Vec3 cameraPos = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        for (WallTV<?> tv : walls) {
            WallManager.observe(tv, frustum);
            if (!tv.isActive()) continue;

            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            WallRenderContext.set(poseStack, camera, bufferSource);
            tv.tick();
            WallRenderContext.clear();

            poseStack.popPose();
        }
    }
}
