package me.kall.narutoloading.agent;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NarutoRenderBridge {
    static final Path NARUTO_JAR;

    private static final AtomicReference<Class<?>> rendererClass = new AtomicReference<>(null);
    private static final AtomicReference<Method> renderMethod = new AtomicReference<>(null);
    private static final AtomicBoolean end = new AtomicBoolean(false);

    static {
        Path narutoJar = null;
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> inputArguments = runtimeMxBean.getInputArguments();
        for (String arg : inputArguments) {
            if (arg.startsWith("-javaagent:")) {
                String agentPath = arg.substring("-javaagent:".length());
                if (agentPath.contains("narutoloading")) {
                    narutoJar = Path.of(agentPath);
                    break;
                }
            }
        }
        NARUTO_JAR = narutoJar;
        if (NARUTO_JAR == null) throw new RuntimeException("NarutoLoading jar not found.");
    }

    @SuppressWarnings("resource")
    private static void ensureInitialized() throws Exception {
        if (rendererClass.get() != null) return;

        synchronized (NarutoRenderBridge.class) {
            if (rendererClass.get() != null) return;

            ClassLoader forgeClassLoader = Thread.currentThread().getContextClassLoader();
            if (forgeClassLoader == null) {
                end.set(true);
                return;
            }

            NarutoClassLoader narutoClassLoader = new NarutoClassLoader(NARUTO_JAR.toUri().toURL(), forgeClassLoader);
            rendererClass.set(narutoClassLoader.loadClass("me.kall.narutoloading.agent.EarlyNarutoRenderer"));
        }
    }

    @SuppressWarnings("unused")
    public static void render() {
        if (end.get()) return;
        try {
            ensureInitialized();
            Class<?> cls = rendererClass.get();
            if (cls == null) return;

            if (renderMethod.get() == null) {
                renderMethod.compareAndSet(null, cls.getMethod("render"));
            }

            renderMethod.get().invoke(null);
        } catch (Throwable throwable) {
            end.set(true);
            throw new RuntimeException(throwable);
        }
    }
}