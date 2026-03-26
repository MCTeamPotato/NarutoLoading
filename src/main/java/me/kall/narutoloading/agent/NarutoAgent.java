package me.kall.narutoloading.agent;

import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class NarutoAgent {
    private static final String[] BOOTSTRAP_ENTRIES = {"me/kall/narutoloading/agent/NarutoRenderBridge.class", "me/kall/narutoloading/agent/NarutoRenderBridge$NarutoClassLoader.class"};

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
            Path tempJar = Files.createTempFile("naruto-bootstrap-", ".jar");
            tempJar.toFile().deleteOnExit();

            try (ZipFile source = new ZipFile(NarutoRenderBridge.NARUTO_JAR.toFile());
                 ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(tempJar.toFile()))) {

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

            return tempJar;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
