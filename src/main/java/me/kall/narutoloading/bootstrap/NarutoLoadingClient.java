package me.kall.narutoloading.bootstrap;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.client.ClientUiControl;
import me.kall.narutoloading.client.input.NarutoKeys;
import me.kall.narutoloading.client.overlay.Substitution;
import me.kall.narutoloading.client.fade.FadeCenter;
import me.kall.narutoloading.client.ui.NarutoScreen;
import me.kall.narutoloading.client.input.PasteListener;
import me.kall.narutoloading.playback.early.EarlySessionHandoff;
import me.kall.narutoloading.wall.client.WallDebugCommand;
import me.kall.narutoloading.wall.client.cull.SurfaceTracker;
import me.kall.narutoloading.wall.client.light.WallLights;
import me.kall.narutoloading.wall.client.WallManager;
import me.kall.narutoloading.wall.client.render.WallRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicLong;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoLoading.MOD_ID)
public class NarutoLoadingClient {
    private static final long KEY_THROTTLE_NANOS = 1_500_000_000L;
    private static final long KEY_MAP_CLEAR_NANOS = 3_000_000_000L;

    private static final Object2LongMap<InputConstants.Key> KEY_LAST_PRESS = Object2LongMaps.synchronize(new Object2LongOpenHashMap<>());
    private static final AtomicLong MAP_LAST_CHANGED = new AtomicLong(System.nanoTime());

    @SubscribeEvent
    public static void tickRender(RenderFrameEvent.@NotNull Pre event) {
        Substitution.tick();
        SurfaceTracker.tick();
        WallLights.tick();
    }

    @SubscribeEvent
    public static void tickClientStart(ClientTickEvent.@NotNull Pre event) {
        FadeCenter.tick();
    }

    @SubscribeEvent
    public static void tickClientEnd(ClientTickEvent.@NotNull Post event) {
        PasteListener.tick();
        WallManager.tick();

        if (System.nanoTime() - MAP_LAST_CHANGED.get() >= KEY_MAP_CLEAR_NANOS) KEY_LAST_PRESS.clear();
    }

    @SubscribeEvent
    public static void renderWalls(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        WallRenderer.render(event.getPoseStack(), event.getCamera(), event.getFrustum());
    }

    @SubscribeEvent
    public static void registerClientCommands(@NotNull RegisterClientCommandsEvent event) {
        WallDebugCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void logOut(ClientPlayerNetworkEvent.@NotNull LoggingOut event) {
        WallManager.clearRemote();
    }

    @SubscribeEvent
    public static void stopEarlySession(ClientPlayerNetworkEvent.@NotNull LoggingIn event) {
        Minecraft.getInstance().execute(EarlySessionHandoff::stop);
    }

    @SubscribeEvent
    public static void shuttingDown(GameShuttingDownEvent event) {
        EarlySessionHandoff.stop();
        ClientUiControl.active().shutdownForExit();
        WallManager.clear();
    }

    @SubscribeEvent
    public static void anyMouseButton(InputEvent.MouseButton.@NotNull Pre event) {
        FadeCenter.reset();
    }

    @SubscribeEvent
    public static void anyMouseButtonReleased(InputEvent.MouseButton.@NotNull Post event) {
        FadeCenter.reset();
    }

    @SubscribeEvent
    public static void anyScroll(InputEvent.@NotNull MouseScrollingEvent event) {
        FadeCenter.reset();
    }

    @SubscribeEvent
    public static void anyInteraction(InputEvent.@NotNull InteractionKeyMappingTriggered event) {
        FadeCenter.reset();
    }

    @SubscribeEvent
    public static void handleKeys(InputEvent.@NotNull Key event) {
        FadeCenter.reset();

        if (event.getAction() != GLFW.GLFW_PRESS) return;

        InputConstants.@NotNull Key key = InputConstants.getKey(event.getKey(), event.getScanCode());
        long now = System.nanoTime();

        if (KEY_LAST_PRESS.containsKey(key) && now - KEY_LAST_PRESS.getLong(key) < KEY_THROTTLE_NANOS) return;

        if (NarutoKeys.OPEN_SCREEN.isActiveAndMatches(key)) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(NarutoScreen.create(minecraft.screen));

            KEY_LAST_PRESS.put(key, now);
            MAP_LAST_CHANGED.set(now);
        }
    }
}
