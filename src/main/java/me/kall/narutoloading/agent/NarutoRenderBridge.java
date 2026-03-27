package me.kall.narutoloading.agent;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

public class NarutoRenderBridge {
    static final Path NARUTO_JAR;

    private static volatile Class<?> rendererClass = null;
    private static volatile Method renderMethod = null;
    private static volatile boolean end = false;

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
        if (rendererClass != null) return;

        ClassLoader forgeClassLoader = Thread.currentThread().getContextClassLoader();
        if (forgeClassLoader == null) {
            end = true;
            return;
        }

        NarutoClassLoader narutoClassLoader = new NarutoClassLoader(NARUTO_JAR.toUri().toURL(), forgeClassLoader);

        rendererClass = narutoClassLoader.loadClass("me.kall.narutoloading.agent.EarlyNarutoRenderer");
    }

    @SuppressWarnings("unused")
    public static void render() {
        if (end) return;
        try {
            ensureInitialized();
            if (rendererClass == null) return;

            if (renderMethod == null) renderMethod = rendererClass.getMethod("render");

            renderMethod.invoke(null);
        } catch (Throwable throwable) {
            end = true;
            throw new RuntimeException(throwable);
        }
    }
}