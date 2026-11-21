package me.kall.narutoloading.agent;

import org.jetbrains.annotations.Nullable;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

public final class DisplayWindowTransformer implements ClassFileTransformer {
    private static final String DISPLAY_WINDOW = "net/minecraftforge/fml/earlydisplay/DisplayWindow";

    private static volatile Method patcher;

    @Override
    public byte @Nullable [] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!DISPLAY_WINDOW.equals(className)) return null;

        try {
            Method patch = patcher();
            return patch == null ? null : (byte[]) patch.invoke(null, classfileBuffer);
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading Agent] Failed to patch DisplayWindow: " + throwable.getMessage());
            return null;
        }
    }

    private static @Nullable Method patcher() {
        Method patch = patcher;
        if (patch != null) return patch;

        synchronized (DisplayWindowTransformer.class) {
            patch = patcher;

            if (patch == null) {
                try {
                    patch = Class.forName("me.kall.narutoloading.agent.DisplayWindowPatcher", true, DisplayWindowTransformer.class.getClassLoader()).getMethod("patch", byte[].class);
                    patcher = patch;
                } catch (ClassNotFoundException | NoSuchMethodException exception) {
                    System.err.println("[NarutoLoading Agent] Patch entry point missing: " + exception.getMessage());
                    return null;
                }
            }
        }

        return patch;
    }
}
