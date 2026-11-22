package me.kall.narutoloading;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.kall.narutoloading.client.ClientUiControl;
import me.kall.narutoloading.client.NarutoKeys;
import me.kall.narutoloading.client.Substitution;
import me.kall.narutoloading.client.context.LevelStatus;
import me.kall.narutoloading.client.ui.NarutoScreen;
import me.kall.narutoloading.client.util.PasteListener;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
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
    public static void tickSubstitution(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) Substitution.tick();
    }

    @SubscribeEvent
    public static void tickPasteListener(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.END)) PasteListener.tick();
    }

    @SubscribeEvent
    public static void clearKeys(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.END) && System.nanoTime() - MAP_LAST_CHANGED.get() >= KEY_MAP_CLEAR_NANOS) KEY_LAST_PRESS.clear();
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
