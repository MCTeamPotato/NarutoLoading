package me.kall.narutoloading.playback;

import me.kall.narutoloading.agent.bootstrap.EarlyBridge;
import me.kall.narutoloading.core.NarutoContext;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class EarlySessionHandoff {
    private static volatile PlaybackSession adopted;

    private static volatile Method detachMethod;
    private static volatile boolean detachChecked;

    private EarlySessionHandoff() {
    }

    public static @Nullable PlaybackSession current() {
        PlaybackSession session = adopted;

        if (session == null) {
            Object raw = System.getProperties().remove(EarlyBridge.SESSION_PROPERTY);
            if (raw == null) return null;

            String earlyMedia = System.getProperty(EarlyBridge.MEDIA_PROPERTY);
            if (earlyMedia != null) {
                System.clearProperty(EarlyBridge.MEDIA_PROPERTY);
                NarutoContext.get().media().adopt(earlyMedia);
            }

            try {
                Class<?> type = raw.getClass();
                session = new Borrowed(raw, type.getMethod("present", boolean.class), type.getMethod("finished"), type.getMethod("close"));
            } catch (Throwable throwable) {
                System.err.println("[NarutoLoading] Unusable early video session: " + throwable.getMessage());
                return null;
            }

            adopted = session;
        }

        if (session.finished()) {
            adopted = null;
            session.close();
            return null;
        }

        return session;
    }

    public static void stop() {
        PlaybackSession session = adopted;
        adopted = null;

        if (session != null) session.close();
    }

    public static void detach() {
        Method detach = detachMethod;

        if (detach == null) {
            if (detachChecked) return;
            detachChecked = true;

            try {
                Class<?> bridge = Class.forName("me.kall.narutoloading.agent.bootstrap.EarlyBridge", true, ClassLoader.getSystemClassLoader());
                detach = bridge.getMethod("detach");
                detachMethod = detach;
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

    private record Borrowed(Object session, Method presentMethod, Method finishedMethod, Method closeMethod) implements PlaybackSession {
        @Override
        public void present(boolean flipY) {
            try {
                this.presentMethod.invoke(this.session, flipY);
            } catch (Throwable throwable) {
                System.err.println("[NarutoLoading] Failed to present the early video: " + throwable.getMessage());
            }
        }

        @Override
        public boolean finished() {
            try {
                return (boolean) this.finishedMethod.invoke(this.session);
            } catch (Throwable throwable) {
                System.err.println("[NarutoLoading] Failed to check the early video: " + throwable.getMessage());
                return false;
            }
        }

        @Override
        public void close() {
            try {
                this.closeMethod.invoke(this.session);
            } catch (Throwable throwable) {
                System.err.println("[NarutoLoading] Failed to close the early video: " + throwable.getMessage());
            }
        }
    }
}
