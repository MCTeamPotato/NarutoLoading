package me.kall.narutoloading.impl;

import me.kall.narutoloading.app.Instance;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.produce.util.TimeLine;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class ClientUiControl {
    private static final String SESSION_PROPERTY = "narutoloading.early.session";

    private static final ClientUiTV.Buffer BUFFER = new ClientUiTV.Buffer();
    private static final ClientUiTV.Image IMAGE = new ClientUiTV.Image();

    private static volatile ClientUiTV<?> active = NarutoConfig.compatMode() ? IMAGE : BUFFER;

    private static volatile Object borrowed;
    private static volatile Method borrowedPresent;
    private static volatile Method borrowedFinished;
    private static volatile Method borrowedClose;

    private static volatile Method earlyDetach;
    private static volatile boolean earlyDetachChecked;

    private ClientUiControl() {
    }

    public static ClientUiTV<?> active() {
        return active;
    }

    public static boolean isCompatMode() {
        return active == IMAGE;
    }

    public static boolean running() {
        if (borrowed() != null) return true;
        return active.prerequisite();
    }

    public static boolean videoPresent() {
        if (borrowed() != null) return true;
        return active.hasVideo();
    }

    public static void render() {
        Object session = borrowed();

        if (session != null) {
            try {
                borrowedPresent.invoke(session, false);
            } catch (Throwable throwable) {
                System.err.println("[NarutoLoading] Failed to present the early video: " + throwable.getMessage());
            }
            return;
        }

        active.tick();
    }

    public static void applyMode() {
        stopBorrowed();

        ClientUiTV<?> target = NarutoConfig.compatMode() ? IMAGE : BUFFER;
        if (target == active) return;

        double seekTo = 0D;
        TimeLine timeLine = active.timeLine.get();
        if (timeLine != null) seekTo = timeLine.positionSec();

        ClientUiTV<?> previous = active;
        active = target;

        previous.shutdown(true);
        target.setup(seekTo);
    }

    public static void stopBorrowed() {
        Object session = borrowed;
        Method close = borrowedClose;

        borrowed = null;
        borrowedPresent = null;
        borrowedFinished = null;
        borrowedClose = null;

        if (session == null || close == null) return;

        try {
            close.invoke(session);
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading] Failed to close the early video: " + throwable.getMessage());
        }
    }

    public static void restart(double seekTo) {
        stopBorrowed();
        active.synchronize(true, seekTo);
    }

    public static void detachEarly() {
        Method detach = earlyDetach;

        if (detach == null) {
            if (earlyDetachChecked) return;
            earlyDetachChecked = true;

            try {
                Class<?> bridge = Class.forName("me.kall.narutoloading.agent.bootstrap.EarlyBridge", true, ClassLoader.getSystemClassLoader());
                detach = bridge.getMethod("detach");
                earlyDetach = detach;
            } catch (Throwable throwable) {
                return;
            }
        }

        try {
            detach.invoke(null);
        } catch (Throwable ignored) {
            // The session was already detached or closed.
        }
    }

    private static @Nullable Object borrowed() {
        Object session = borrowed;

        if (session == null) {
            session = System.getProperties().remove(SESSION_PROPERTY);
            if (session == null) return null;

            String earlyMedia = System.getProperty("narutoloading.early.media");
            if (earlyMedia != null) {
                System.clearProperty("narutoloading.early.media");
                Instance.get().media.adopt(earlyMedia);
            }

            try {
                Class<?> type = session.getClass();
                borrowedPresent = type.getMethod("present", boolean.class);
                borrowedFinished = type.getMethod("finished");
                borrowedClose = type.getMethod("close");
            } catch (Throwable throwable) {
                System.err.println("[NarutoLoading] Unusable early video session: " + throwable.getMessage());
                return null;
            }

            borrowed = session;
        }

        try {
            if ((boolean) borrowedFinished.invoke(session)) {
                borrowed = null;
                borrowedClose.invoke(session);
                return null;
            }
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading] Failed to check the early video: " + throwable.getMessage());
        }

        return session;
    }
}
