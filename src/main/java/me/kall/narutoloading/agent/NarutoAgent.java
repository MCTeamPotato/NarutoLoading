package me.kall.narutoloading.agent;

import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class NarutoAgent {
    private static final String[] BOOTSTRAP_ENTRIES = {"me/kall/narutoloading/agent/NarutoRenderBridge.class", "me/kall/narutoloading/agent/NarutoClassLoader.class"};
    private static final Path NARUTO_JAR;

    public static void premain(String agentArgs, @NotNull Instrumentation instrumentation) {
        instrumentation.addTransformer(new NarutoTransformer(), false);
        try {
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(createBootstrapOnlyJar().toFile()));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static @NotNull Path createBootstrapOnlyJar() {
        try {
            Path narutoBootstrap = Path.of("D:/HMCL/.minecraft/versions/1.20.1-Forge").resolve("mods").resolve("naruto-bootstrap.jar");
            if (narutoBootstrap.toFile().exists() && narutoBootstrap.toFile().delete()) System.out.println("Deleting existing naruto-bootstrap.jar for update");
            Files.createFile(narutoBootstrap);

            try (ZipFile source = new ZipFile(NARUTO_JAR.toFile());
                 ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(narutoBootstrap.toFile()))) {

                for (String entryName : BOOTSTRAP_ENTRIES) {
                    ZipEntry zipEntry = source.getEntry(entryName);
                    if (zipEntry == null) continue;
                    zipOutputStream.putNextEntry(new ZipEntry(entryName));
                    try (InputStream inputStream = source.getInputStream(zipEntry)) {
                        inputStream.transferTo(zipOutputStream);
                    }
                    zipOutputStream.closeEntry();
                }
            }

            return narutoBootstrap;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

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
}
