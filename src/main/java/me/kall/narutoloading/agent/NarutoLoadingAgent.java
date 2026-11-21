package me.kall.narutoloading.agent;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class NarutoLoadingAgent {
    private static final String[] BOOTSTRAP_CLASSES = {"me/kall/narutoloading/agent/bootstrap/EarlyBridge.class", "me/kall/narutoloading/agent/bootstrap/EarlyVideo.class"};

    private NarutoLoadingAgent() {
    }

    public static void premain(String args, Instrumentation instrumentation) {
        try {
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(createBootstrapJar().toFile()));
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading Agent] Failed to install the bootstrap classes: " + throwable.getMessage());
            return;
        }

        instrumentation.addTransformer(new DisplayWindowTransformer(), false);
        EarlyInstaller.install();
    }

    private static @NotNull Path createBootstrapJar() throws IOException {
        Path jar = Files.createTempFile("narutoloading-bootstrap", ".jar");
        jar.toFile().deleteOnExit();

        ClassLoader loader = NarutoLoadingAgent.class.getClassLoader();

        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (String entry : BOOTSTRAP_CLASSES) {
                try (InputStream input = loader.getResourceAsStream(entry)) {
                    if (input == null) throw new IOException("Missing " + entry);

                    output.putNextEntry(new ZipEntry(entry));
                    input.transferTo(output);
                    output.closeEntry();
                }
            }
        }

        return jar;
    }
}
