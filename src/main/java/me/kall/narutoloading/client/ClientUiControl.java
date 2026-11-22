package me.kall.narutoloading.client;

import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.playback.ClientUiTV;
import me.kall.narutoloading.playback.early.EarlySessionHandoff;
import me.kall.narutoloading.playback.early.PlaybackSession;
import me.kall.narutoloading.playback.produce.util.TimeLine;

public final class ClientUiControl {
    private ClientUiControl() {
    }

    public static ClientUiTV<?> active() {
        return NarutoContext.get().clientUi();
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
        PlaybackSession session = EarlySessionHandoff.current();
        if (session != null) return session.positionSec();

        TimeLine timeLine = active().timeLine.get();
        return timeLine == null ? 0D : timeLine.positionSec();
    }

    public static void detachEarly() {
        EarlySessionHandoff.detach();
    }
}
