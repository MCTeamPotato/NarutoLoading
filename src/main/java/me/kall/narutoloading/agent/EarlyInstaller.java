package me.kall.narutoloading.agent;

import me.kall.narutoloading.agent.bootstrap.EarlyBridge;
import me.kall.narutoloading.agent.bootstrap.EarlyVideo;

final class EarlyInstaller {
    private EarlyInstaller() {
    }

    static void install() {
        EarlyBridge.install(EarlyInstaller::create);
    }

    private static EarlyVideo create(ClassLoader parent) {
        if (parent == null) return null;

        try {
            return (EarlyVideo) Class.forName("me.kall.narutoloading.agent.EarlyResolver", true, EarlyInstaller.class.getClassLoader()).getMethod("create", ClassLoader.class).invoke(null, parent);
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading Agent] Failed to create the early video: " + throwable.getMessage());
            return null;
        }
    }
}
