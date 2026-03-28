package me.kall.narutoloading.agent;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public class NarutoRenderBridge {
    public static final Path NARUTO_JAR;

    private static final AtomicReference<Class<?>> RENDERER_CLASS = new AtomicReference<>(null);
    private static final AtomicReference<Method> RENDER_METHOD = new AtomicReference<>(null);
    private static final AtomicReference<Method> SHUTDOWN_METHOD = new AtomicReference<>(null);

    static {
        Path narutoJar = null;
        for (String jvmArgument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (jvmArgument.startsWith("-javaagent:")) {
                String agentPath = jvmArgument.substring("-javaagent:".length());
                if (agentPath.contains("narutoloading")) {
                    narutoJar = Path.of(agentPath);
                    break;
                }
            }
        }
        NARUTO_JAR = narutoJar;
    }

    @SuppressWarnings("resource")
    private static void ensureInitialized() throws Exception {
        if (RENDERER_CLASS.get() != null) return;

        synchronized (NarutoRenderBridge.class) {
            if (RENDERER_CLASS.get() != null) return;
            NarutoClassLoader narutoClassLoader = new NarutoClassLoader(NARUTO_JAR.toUri().toURL(), Thread.currentThread().getContextClassLoader());
            RENDERER_CLASS.set(narutoClassLoader.loadClass("me.kall.narutoloading.agent.EarlyNarutoRenderer"));
        }
    }

    public static void render() {
        try {
            ensureInitialized();
            if (RENDER_METHOD.get() == null) RENDER_METHOD.compareAndSet(null, RENDERER_CLASS.get().getMethod("render"));
            RENDER_METHOD.get().invoke(null);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}