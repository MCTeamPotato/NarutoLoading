package me.kall.narutoloading.agent;

import org.jetbrains.annotations.Nullable;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

public final class DisplayWindowTransformer implements ClassFileTransformer {
    private static final String DISPLAY_WINDOW = "net/minecraftforge/fml/earlydisplay/DisplayWindow";
    private static final String EARLY_FRAMEBUFFER = "net/minecraftforge/fml/earlydisplay/EarlyFramebuffer";

    private static volatile Method windowPatcher;
    private static volatile Method framebufferPatcher;

    @Override
    public byte @Nullable [] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (DISPLAY_WINDOW.equals(className)) {
            return apply(DISPLAY_WINDOW, windowPatcher(), classfileBuffer);
        }

        if (EARLY_FRAMEBUFFER.equals(className)) {
            return apply(EARLY_FRAMEBUFFER, framebufferPatcher(), classfileBuffer);
        }

        return null;
    }

    private static byte @Nullable [] apply(String target, @Nullable Method patcher, byte[] classfileBuffer) {
        if (patcher == null) return null;

        try {
            return (byte[]) patcher.invoke(null, (Object) classfileBuffer);
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading Agent] Failed to patch " + target + ": " + throwable.getMessage());
            return null;
        }
    }

    private static @Nullable Method windowPatcher() {
        Method patcher = windowPatcher;
        if (patcher != null) return patcher;

        synchronized (DisplayWindowTransformer.class) {
            patcher = windowPatcher;

            if (patcher == null) {
                patcher = load("me.kall.narutoloading.agent.DisplayWindowPatcher");
                windowPatcher = patcher;
            }
        }

        return patcher;
    }

    private static @Nullable Method framebufferPatcher() {
        Method patcher = framebufferPatcher;
        if (patcher != null) return patcher;

        synchronized (DisplayWindowTransformer.class) {
            patcher = framebufferPatcher;

            if (patcher == null) {
                patcher = load("me.kall.narutoloading.agent.EarlyFramebufferPatcher");
                framebufferPatcher = patcher;
            }
        }

        return patcher;
    }

    private static @Nullable Method load(String name) {
        try {
            return Class.forName(name, true, DisplayWindowTransformer.class.getClassLoader()).getMethod("patch", byte[].class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            System.err.println("[NarutoLoading Agent] Patch entry point missing: " + exception.getMessage());
            return null;
        }
    }
}
