package me.kall.narutoloading.client;

import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.playback.ClientUiTV;
import me.kall.narutoloading.playback.early.EarlySessionHandoff;
import me.kall.narutoloading.playback.early.PlaybackSession;
import me.kall.narutoloading.playback.produce.util.TimeLine;

public final class ClientUiControl {
    private static final double PREWARM_SECONDS = 0.8D;

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
            if (session.durationSec() - session.positionSec() <= PREWARM_SECONDS) active().prewarm(active().newVideoArgs());

            session.present(false);
            return;
        }

        active().tick();
    }

    public static void restart(double seekTo) {
        EarlySessionHandoff.stop();
        active().synchronize(true, seekTo);
    }

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
