package me.kall.narutoloading.core;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.core.detection.inworld.ScreenChecker;
import me.kall.narutoloading.data.VideoArgs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NarutoInWorldRenderer extends NarutoRenderer {
    public static final NarutoInWorldRenderer INSTANCE = new NarutoInWorldRenderer();
    public static final Object2ObjectMap<ResourceLocation, ObjectSet<ScreenChecker.Screen>> SCREENS = new Object2ObjectOpenHashMap<>();

    public void onRenderTick(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START && this.isRunning()) {
            this.renderFrame(null);
        }
    }

    public void onRenderLevel(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        ResourceLocation dimension = level.dimension().location();
        ObjectSet<ScreenChecker.Screen> screens = SCREENS.get(dimension);
        if (screens == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Vec3 cameraPos = event.getCamera().getPosition();

        for (ScreenChecker.Screen screen : screens) {
            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            screen.renderImage(poseStack, bufferSource, this.nextFrame());
            poseStack.popPose();
        }
    }

    @Override
    public void renderFrame(@Nullable GuiGraphics graphics) {
        if (this.isEnabled()) {
            this.lifetime.tick();
            this.keyChecker.reload();
            this.lifetime.lagSpikeRestart();
            this.lifetime.endRestart();
        } else {
            this.shutdown();
        }
    }

    @Override
    public boolean isEnabled() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GenericDirtMessageScreen) return false;
        if (VideoArgs.width() == 0 || VideoArgs.height() == 0) return false;
        return minecraft.level != null && minecraft.isRunning();
    }

}
