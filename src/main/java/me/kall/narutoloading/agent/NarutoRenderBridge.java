package me.kall.narutoloading.agent;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;

public class NarutoRenderBridge {
    static final Path NARUTO_JAR;

    private static volatile Method renderMethod = null;
    private static volatile Method shutdownMethod = null;
    private static volatile boolean failed = false;

    static {
        try {
            NARUTO_JAR = Path.of("D:/HMCL/.minecraft/versions/1.20.1-Forge/mods").resolve("narutoloading-1.20.1-3.0.0.jar");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @SuppressWarnings({"resource", "unused"})
    public static void render() {
        if (failed) return;
        try {
            if (renderMethod == null) {
                ClassLoader forgeClassLoader = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(frames -> frames.map(stackFrame -> stackFrame.getDeclaringClass().getClassLoader()).filter(Objects::nonNull).findFirst()).orElse(null);
                if (forgeClassLoader == null) {
                    failed = true;
                    return;
                }

                NarutoClassLoader narutoClassLoader = new NarutoClassLoader(NARUTO_JAR.toUri().toURL(), forgeClassLoader);
                Class<?> rendererClass = narutoClassLoader.loadClass("me.kall.narutoloading.agent.EarlyNarutoRenderer");
                renderMethod = rendererClass.getMethod("render");
            }
            renderMethod.invoke(null);
        } catch (Throwable throwable) {
            failed = true;
            throw new RuntimeException(throwable);
        }
    }

    @SuppressWarnings("unused")
    public static void shutdown() {
        if (failed) return;
        try {
            if (renderMethod == null) return;
            if (shutdownMethod == null) {
                shutdownMethod = renderMethod.getDeclaringClass().getMethod("shutdown");
            }
            shutdownMethod.invoke(null);
        } catch (Throwable throwable) {
            failed = true;
            throw new RuntimeException(throwable);
        }
    }

    public static final class NarutoClassLoader extends URLClassLoader {
        private final ClassLoader forgClassLoader;

        private static final String LWJGL_PACKAGE = "org.lwjgl.";

        public NarutoClassLoader(URL jarUrl, ClassLoader forgClassLoader) {
            super(new URL[]{jarUrl}, forgClassLoader);
            this.forgClassLoader = forgClassLoader;
        }

        @Override
        public Class<?> loadClass(@NotNull String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith(LWJGL_PACKAGE)) return this.forgClassLoader.loadClass(name);

            synchronized (this.getClassLoadingLock(name)) {
                Class<?> loadedClass = this.findLoadedClass(name);
                if (loadedClass != null) return loadedClass;

                try {
                    loadedClass = this.findClass(name);
                    if (resolve) this.resolveClass(loadedClass);
                    return loadedClass;
                } catch (ClassNotFoundException ignored) {
                    return this.forgClassLoader.loadClass(name);
                }
            }
        }
    }
}
