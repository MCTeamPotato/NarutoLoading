package me.kall.narutoloading.agent;

import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.net.URLClassLoader;

final class EarlyClassLoader extends URLClassLoader {
    private static final String MOD_PACKAGE = "me.kall.narutoloading.";
    private static final String BOOTSTRAP_PACKAGE = "me.kall.narutoloading.agent.bootstrap.";

    EarlyClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!isOwn(name)) return super.loadClass(name, resolve);

        synchronized (this.getClassLoadingLock(name)) {
            Class<?> loaded = this.findLoadedClass(name);

            if (loaded == null) {
                try {
                    loaded = this.findClass(name);
                } catch (ClassNotFoundException exception) {
                    loaded = super.loadClass(name, false);
                }
            }

            if (resolve) this.resolveClass(loaded);
            return loaded;
        }
    }

    private static boolean isOwn(@NotNull String name) {
        if (name.startsWith(BOOTSTRAP_PACKAGE)) return false;
        return name.startsWith(MOD_PACKAGE);
    }
}
