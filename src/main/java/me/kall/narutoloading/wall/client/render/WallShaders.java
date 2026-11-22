package me.kall.narutoloading.wall.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import me.kall.narutoloading.NarutoLoading;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WallShaders {
    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "yuv_world");

    private static @Nullable ShaderInstance shader;

    private WallShaders() {}

    public static @Nullable ShaderInstance shader() {
        return shader;
    }

    public static boolean available() {
        return shader != null;
    }

    public static void register(@NotNull RegisterShadersEvent event) {
        shader = null;

        try {
            ShaderInstance instance = new ShaderInstance(event.getResourceProvider(), SHADER, DefaultVertexFormat.POSITION_TEX);
            event.registerShader(instance, loaded -> shader = loaded);
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading] Could not load the world yuv shader: " + throwable.getMessage());
        }
    }
}
