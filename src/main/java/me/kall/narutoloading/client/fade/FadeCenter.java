package me.kall.narutoloading.client.fade;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.config.NarutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoLoading.MOD_ID)
public final class FadeCenter {
    private static final MousePos LAST_MOUSE = new MousePos();

    private static volatile @Nullable BooleanSupplier previewEnabled;
    private static volatile @Nullable IntSupplier previewTicks;

    private static int stopTicks = 0;
    private static int fadeAlpha = 100;
    private static boolean itemFade = false;

    public static void preview(@NotNull BooleanSupplier enabled, @NotNull IntSupplier ticksBeforeFade) {
        previewEnabled = enabled;
        previewTicks = ticksBeforeFade;
    }

    public static void clearPreview() {
        previewEnabled = null;
        previewTicks = null;
    }

    public static boolean enabled() {
        BooleanSupplier preview = previewEnabled;
        return preview == null ? NarutoConfig.fadable() : preview.getAsBoolean();
    }

    public static int ticksBeforeFade() {
        IntSupplier preview = previewTicks;
        return preview == null ? NarutoConfig.ticksBeforeFade() : preview.getAsInt();
    }

    public static boolean isHidden() {
        return fadeAlpha == 0;
    }

    public static boolean isFull() {
        return fadeAlpha == 100;
    }

    public static float fadeAlpha() {
        return (float) fadeAlpha / 100.0F;
    }

    public static int modifyAlpha(int color) {
        if (isFull()) return color;
        int alpha = (int) (((color >>> 24) & 0xFF) * fadeAlpha()) & 0xFF;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public static int modifyTextAlpha(int color) {
        if (isFull()) return color;
        int alpha = (color >>> 24) & 0xFF;
        if (alpha == 0) alpha = 0xFF;
        alpha = (int) (alpha * fadeAlpha()) & 0xFF;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @SubscribeEvent
    public static void tick(TickEvent.@NotNull ClientTickEvent event) {
        if (!event.phase.equals(TickEvent.Phase.START)) return;

        if (!enabled()) {
            stopTicks = 0;
            fadeAlpha = 100;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        MouseHandler mouseHandler = minecraft.mouseHandler;

        double x = mouseHandler.xpos();
        double y = mouseHandler.ypos();

        if (minecraft.level == null && minecraft.player == null && minecraft.screen != null && minecraft.getOverlay() == null && !LAST_MOUSE.init(x, y)) {
            stopTicks = LAST_MOUSE.same(x, y) ? stopTicks + 1 : 0;
            LAST_MOUSE.set(x, y);
        } else {
            stopTicks = 0;
        }

        fadeAlpha = stopTicks >= ticksBeforeFade() ? fadeAlpha - 1 : fadeAlpha + 5;
        if (fadeAlpha < 0) fadeAlpha = 0;
        if (fadeAlpha > 100) fadeAlpha = 100;
    }

    @SubscribeEvent
    public static void anyInput(InputEvent event) {
        Minecraft.getInstance().execute(() -> stopTicks = 0);
    }

    @SubscribeEvent
    public static void mouseInput(InputEvent.MouseButton event) {
        Minecraft.getInstance().execute(() -> fadeAlpha = 100);
    }

    private static final class MousePos {
        double x = Double.NaN;
        double y = Double.NaN;

        boolean same(double x, double y) {
            return this.x == x && this.y == y;
        }

        boolean init(double x, double y) {
            if (Double.isNaN(this.x) || Double.isNaN(this.y)) {
                this.x = x;
                this.y = y;
                return true;
            }

            return false;
        }

        void set(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
