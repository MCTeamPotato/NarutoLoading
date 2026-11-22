package me.kall.narutoloading.client.fade;

import me.kall.narutoloading.client.ClientUiControl;
import me.kall.narutoloading.client.context.LevelStatus;
import me.kall.narutoloading.config.NarutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public final class FadeCenter {
    private static final MousePos LAST_MOUSE = new MousePos();

    private static volatile @Nullable BooleanSupplier previewEnabled;
    private static volatile @Nullable IntSupplier previewTicks;
    private static volatile @Nullable IntSupplier previewFadeOutStep;
    private static volatile @Nullable IntSupplier previewFadeInStep;

    private static int stopTicks = 0;
    private static int fadeAlpha = 100;

    public static void preview(@NotNull BooleanSupplier enabled, @NotNull IntSupplier ticksBeforeFade, @NotNull IntSupplier fadeOutStep, @NotNull IntSupplier fadeInStep) {
        previewEnabled = enabled;
        previewTicks = ticksBeforeFade;
        previewFadeOutStep = fadeOutStep;
        previewFadeInStep = fadeInStep;
    }

    public static void clearPreview() {
        previewEnabled = null;
        previewTicks = null;
        previewFadeOutStep = null;
        previewFadeInStep = null;
    }

    public static boolean enabled() {
        BooleanSupplier preview = previewEnabled;
        return preview == null ? NarutoConfig.fadable() : preview.getAsBoolean();
    }

    public static int ticksBeforeFade() {
        IntSupplier preview = previewTicks;
        return preview == null ? NarutoConfig.ticksBeforeFade() : preview.getAsInt();
    }

    public static int fadeOutStep() {
        IntSupplier preview = previewFadeOutStep;
        return preview == null ? NarutoConfig.fadeOutStep() : preview.getAsInt();
    }

    public static int fadeInStep() {
        IntSupplier preview = previewFadeInStep;
        return preview == null ? NarutoConfig.fadeInStep() : preview.getAsInt();
    }

    public static boolean isHidden() {
        return LevelStatus.notFound() && fadeAlpha < 3;
    }

    public static boolean isFull() {
        return !LevelStatus.notFound() || fadeAlpha == 100;
    }

    public static float fadeAlpha() {
        if (!LevelStatus.notFound()) return 1.0F;
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

    public static void tick() {
        if (!LevelStatus.notFound() || !ClientUiControl.videoPresent() || !enabled()) {
            fadeAlpha = 100;
            stopTicks = 0;
            LAST_MOUSE.set(Double.NaN, Double.NaN);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        MouseHandler mouseHandler = minecraft.mouseHandler;

        double x = mouseHandler.xpos();
        double y = mouseHandler.ypos();

        if (minecraft.screen != null && minecraft.getOverlay() == null && !LAST_MOUSE.init(x, y)) {
            stopTicks = LAST_MOUSE.same(x, y) ? stopTicks + 1 : 0;
            LAST_MOUSE.set(x, y);
        } else {
            stopTicks = 0;
        }

        fadeAlpha = stopTicks >= ticksBeforeFade() ? fadeAlpha - Math.max(0, fadeOutStep()) : fadeAlpha + Math.max(0, fadeInStep());
        if (fadeAlpha < 0) fadeAlpha = 0;
        if (fadeAlpha > 100) fadeAlpha = 100;
    }

    public static void reset() {
        Minecraft.getInstance().execute(() -> {
            fadeAlpha = 100;
            stopTicks = 0;
            LAST_MOUSE.set(Double.NaN, Double.NaN);
        });
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
