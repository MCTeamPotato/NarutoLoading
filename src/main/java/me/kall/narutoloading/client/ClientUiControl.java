package me.kall.narutoloading.client;

import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.config.NarutoSettings;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.playback.ClientUiTV;
import me.kall.narutoloading.playback.EarlySessionHandoff;
import me.kall.narutoloading.playback.PlaybackSession;
import me.kall.narutoloading.playback.produce.util.TimeLine;

import java.util.Map;

public final class ClientUiControl {
    private ClientUiControl() {
    }

    public static ClientUiTV<?> active() {
        return NarutoContext.get().modes().active();
    }

    public static boolean isCompatMode() {
        return NarutoContext.get().modes().isCompatMode();
    }

    public static boolean running() {
        if (EarlySessionHandoff.current() != null) return true;
        return active().prerequisite();
    }

    public static boolean videoPresent() {
        if (EarlySessionHandoff.current() != null) return true;
        return active().hasVideo();
    }

    public static void render() {
        PlaybackSession session = EarlySessionHandoff.current();

        if (session != null) {
            session.present(false);
            return;
        }

        active().tick();
    }

    public static void applyMode() {
        EarlySessionHandoff.stop();
        NarutoContext.get().modes().apply();
    }

    public static void toggleCompatMode() {
        NarutoConfig.writeAll(Map.of(NarutoSettings.COMPAT_MODE, !NarutoConfig.compatMode()));
        NarutoConfig.save();
        applyMode();
    }

    public static void restart(double seekTo) {
        EarlySessionHandoff.stop();
        active().synchronize(true, seekTo);
    }

    /**
     * Returns the playback position of the active session, or {@code 0} when nothing is playing.
     * Use it to restart the video at the point the player was watching instead of rewinding it.
     *
     * @return the current position in seconds
     */
    public static double positionSec() {
        TimeLine timeLine = active().timeLine.get();
        return timeLine == null ? 0D : timeLine.positionSec();
    }

    public static void detachEarly() {
        EarlySessionHandoff.detach();
    }
}
