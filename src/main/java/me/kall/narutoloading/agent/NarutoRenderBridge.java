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

    private static final AtomicReference<Class<?>> RENDERER_CLASS = new AtomicReference<>(null);
    private static final AtomicReference<Method> RENDER_METHOD = new AtomicReference<>(null);

    public static final AtomicBoolean END = new AtomicBoolean(false);

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
        if (RENDERER_CLASS.get() != null) return;

        synchronized (NarutoRenderBridge.class) {
            if (RENDERER_CLASS.get() != null) return;

            ClassLoader forgeClassLoader = Thread.currentThread().getContextClassLoader();
            if (forgeClassLoader == null) throw new RuntimeException("Forge classloader not found");

            NarutoClassLoader narutoClassLoader = new NarutoClassLoader(NARUTO_JAR.toUri().toURL(), forgeClassLoader);
            RENDERER_CLASS.set(narutoClassLoader.loadClass("me.kall.narutoloading.agent.EarlyNarutoRenderer"));
        }
    }

    @SuppressWarnings("unused")
    public static void render() {
        if (END.get()) return;
        try {
            ensureInitialized();
            if (RENDER_METHOD.get() == null) RENDER_METHOD.compareAndSet(null, RENDERER_CLASS.get().getMethod("render"));
            RENDER_METHOD.get().invoke(null);
        } catch (Throwable throwable) {
            END.set(true);
            throw new RuntimeException(throwable);
        }
    }
}