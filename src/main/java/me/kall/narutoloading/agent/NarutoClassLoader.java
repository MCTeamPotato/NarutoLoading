package me.kall.narutoloading.agent;

import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.net.URLClassLoader;

public final class NarutoClassLoader extends URLClassLoader {
    private final ClassLoader contextClassLoader;

    private static final String LWJGL_PACKAGE = "org.lwjgl.";

    public NarutoClassLoader(URL jarUrl, ClassLoader contextClassLoader) {
        super(new URL[]{jarUrl}, contextClassLoader);
        this.contextClassLoader = contextClassLoader;
    }

    @Override
    public Class<?> loadClass(@NotNull String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith(LWJGL_PACKAGE)) return this.contextClassLoader.loadClass(name);

        synchronized (this.getClassLoadingLock(name)) {
            Class<?> loadedClass = this.findLoadedClass(name);
            if (loadedClass != null) return loadedClass;

            try {
                loadedClass = this.findClass(name);
                if (resolve) this.resolveClass(loadedClass);
                return loadedClass;
            } catch (ClassNotFoundException ignored) {
                return this.contextClassLoader.loadClass(name);
            }
        }
    }
}
