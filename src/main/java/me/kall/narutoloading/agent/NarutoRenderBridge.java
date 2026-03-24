package me.kall.narutoloading.agent;

import java.nio.file.Path;
import java.util.Objects;

public final class NarutoRenderBridge {
    public static final String NARUTO_JAR_PATH = Path.of("D:/HMCL/.minecraft/versions/1.20.1-Forge").resolve("mods").resolve("narutoloading-1.20.1-2.2.3.jar").toString().toString();

    private static volatile java.lang.reflect.Method renderMethod = null;
    private static volatile boolean failed = false;

    private static final class NarutoClassLoader extends java.net.URLClassLoader {
        private final ClassLoader forgeCL;

        NarutoClassLoader(java.net.URL jarUrl, ClassLoader forgeCL) {
            super(new java.net.URL[]{jarUrl}, null);
            this.forgeCL = forgeCL;
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("org.lwjgl.")) {
                return forgeCL.loadClass(name);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c != null) return c;
                try {
                    c = findClass(name);
                    if (resolve) resolveClass(c);
                    return c;
                } catch (ClassNotFoundException ignored) {
                    return forgeCL.loadClass(name);
                }
            }
        }
    }

    public static void render() {
        if (failed) return;
        try {
            if (renderMethod == null) {
                ClassLoader forgeCL = StackWalker
                        .getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                        .walk(frames -> frames
                                .map(f -> f.getDeclaringClass().getClassLoader())
                                .filter(Objects::nonNull)
                                .findFirst()
                        ).orElse(null);

                if (forgeCL == null) {
                    failed = true;
                    return;
                }

                java.net.URL jarUrl = java.nio.file.Path.of(NARUTO_JAR_PATH).toUri().toURL();
                @SuppressWarnings("resource")
                NarutoClassLoader narutoLoader = new NarutoClassLoader(jarUrl, forgeCL);

                Class<?> cls = narutoLoader.loadClass("me.kall.narutoloading.agent.NarutoBackgroundHelper");
                renderMethod = cls.getMethod("render");
            }
            renderMethod.invoke(null);
        } catch (Throwable t) {
            failed = true;
            t.printStackTrace();
        }
    }
}