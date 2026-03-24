package me.kall.narutoloading.agent;

import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.util.Paths;
import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

public class NarutoAgent {
    public static void premain(String agentArgs, @NotNull Instrumentation inst) {
        System.out.println("[NarutoAgent] premain loaded successfully.");

        BaseEnv.setupEnv(true);
        if (BaseEnv.available()) {
            System.out.println("[NarutoAgent] BaseEnv loaded successfully.");
            System.out.println("[NarutoAgent] FFmpeg: " + BaseEnv.ffmpegProvider.absoluteFFmpeg);
            System.out.println("[NarutoAgent] FFProbe: " + BaseEnv.ffmpegProvider.absoluteFFprobe);
            System.out.println("[NarutoAgent] NarutoConfig: " + BaseEnv.narutoConfig.toString());
            System.out.println("[NarutoAgent] YtDlp: " + BaseEnv.ytDlpProvider.absoluteYtDlp);
        }

        inst.addTransformer(new DisplayWindowTransformer(), false);

        try {
            JarFile jarFile = new JarFile(Paths.GAME_DIR.resolve("mods").resolve("narutoloading-1.20.1-2.2.3.jar").toFile());
            inst.appendToBootstrapClassLoaderSearch(jarFile);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
