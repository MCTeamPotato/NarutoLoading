package me.kall.narutoloading.agent.bootstrap;

import java.util.function.Function;

public final class EarlyBridge {
    public static final String ACTIVE_PROPERTY = "narutoloading.early";
    public static final String SESSION_PROPERTY = "narutoloading.early.session";
    public static final String MEDIA_PROPERTY = "narutoloading.early.media";

    private static volatile Function<ClassLoader, EarlyVideo> factory;
    private static volatile EarlyVideo video;
    private static volatile boolean created;

    private EarlyBridge() {
    }

    public static void install(Function<ClassLoader, EarlyVideo> earlyFactory) {
        factory = earlyFactory;
        System.setProperty(ACTIVE_PROPERTY, "1");
    }

    public static void render(ClassLoader loader) {
        EarlyVideo current = video;

        if (current == null) {
            if (created) return;
            created = true;

            Function<ClassLoader, EarlyVideo> earlyFactory = factory;
            if (earlyFactory == null) return;

            try {
                current = earlyFactory.apply(loader);
            } catch (Throwable throwable) {
                System.err.println("[NarutoLoading Agent] Failed to create the early video: " + throwable.getMessage());
                return;
            }

            video = current;
            if (current == null) return;
        }

        current.present(true);
    }

    @SuppressWarnings("unused")
    public static void detach() {
        factory = null;
        System.clearProperty(ACTIVE_PROPERTY);

        EarlyVideo current = video;
        video = null;

        if (current != null) System.getProperties().put(SESSION_PROPERTY, current);
    }
}
