package me.kall.narutoloading.agent;

import me.kall.narutoloading.common.env.BaseEnv;
import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.jar.JarFile;

public class NarutoAgent {
    public static void premain(String agentArgs, @NotNull Instrumentation inst) {
        BaseEnv.setupEnv(true);
        if (BaseEnv.available()) {
            System.out.println("[NarutoAgent] BaseEnv loaded successfully.");
            System.out.println("[NarutoAgent] FFmpeg: " + BaseEnv.getFfmpegProvider().absoluteFFmpeg);
            System.out.println("[NarutoAgent] FFProbe: " + BaseEnv.getFfmpegProvider().absoluteFFprobe);
            System.out.println("[NarutoAgent] NarutoConfig: " + BaseEnv.getNarutoConfig().toString());
            System.out.println("[NarutoAgent] YtDlp: " + BaseEnv.getYtDlpProvider().absoluteYtDlp);
        }

        inst.addTransformer(new DisplayWindowTransformer(), false);

        try {
            inst.appendToBootstrapClassLoaderSearch(new JarFile(createBootstrapOnlyJar(Path.of("D:/HMCL/.minecraft/versions/1.20.1-Forge").resolve("mods").resolve("narutoloading-1.20.1-2.2.3.jar")).toFile()));

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static @NotNull Path createBootstrapOnlyJar(Path sourceJar) throws Exception {
        Path tempJar = java.nio.file.Files.createTempFile("naruto-bootstrap-", ".jar");
        tempJar.toFile().deleteOnExit();

        String[] entries = {"me/kall/narutoloading/agent/NarutoRenderBridge.class", "me/kall/narutoloading/agent/NarutoRenderBridge$NarutoClassLoader.class"};

        try (java.util.zip.ZipFile source = new java.util.zip.ZipFile(sourceJar.toFile());
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(tempJar.toFile()))) {

            for (String entryName : entries) {
                java.util.zip.ZipEntry entry = source.getEntry(entryName);
                if (entry == null) continue;
                zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                try (java.io.InputStream is = source.getInputStream(entry)) {
                    is.transferTo(zos);
                }
                zos.closeEntry();
            }
        }

        return tempJar;
    }
}
