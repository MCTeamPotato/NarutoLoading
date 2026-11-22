package me.kall.narutoloading.bootstrap;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.client.ClientUiControl;
import me.kall.narutoloading.client.input.NarutoKeys;
import me.kall.narutoloading.client.overlay.Substitution;
import me.kall.narutoloading.client.context.LevelStatus;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicLong;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoLoading.MOD_ID)
public class NarutoLoadingClient {
    private static final long KEY_THROTTLE_NANOS = 1_500_000_000L;
    private static final long KEY_MAP_CLEAR_NANOS = 3_000_000_000L;

    private static final Object2LongMap<InputConstants.Key> KEY_LAST_PRESS = Object2LongMaps.synchronize(new Object2LongOpenHashMap<>());
    private static final AtomicLong MAP_LAST_CHANGED = new AtomicLong(System.nanoTime());

    @SubscribeEvent
    public static void tickRender(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Substitution.tick();
        SurfaceTracker.tick();
        WallLights.tick();
    }

    @SubscribeEvent
    public static void tickClientStart(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) FadeCenter.tick();
    }

    @SubscribeEvent
    public static void tickClientEnd(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

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
    public static void anyInput(@NotNull InputEvent event) {
        FadeCenter.reset();
    }

    @SubscribeEvent
    public static void mouseInput(InputEvent.MouseButton.@NotNull Pre event) {
        FadeCenter.reset();
    }

    @SubscribeEvent
    public static void handleKeys(InputEvent.@NotNull Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        InputConstants.@NotNull Key key = InputConstants.getKey(event.getKey(), event.getScanCode());
        long now = System.nanoTime();

        if (KEY_LAST_PRESS.containsKey(key) && now - KEY_LAST_PRESS.getLong(key) < KEY_THROTTLE_NANOS) return;

        if (NarutoKeys.TOGGLE_COMPAT_MODE.isActiveAndMatches(key) && LevelStatus.notFound()) {
            ClientUiControl.toggleCompatMode();

            KEY_LAST_PRESS.put(key, now);
            MAP_LAST_CHANGED.set(now);
            return;
        }

        if (NarutoKeys.OPEN_SCREEN.isActiveAndMatches(key) && Integration.NARUTO_UI) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(NarutoScreen.create(minecraft.screen));

            KEY_LAST_PRESS.put(key, now);
            MAP_LAST_CHANGED.set(now);
        }
    }
}
