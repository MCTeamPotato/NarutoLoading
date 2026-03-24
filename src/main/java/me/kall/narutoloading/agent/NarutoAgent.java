package me.kall.narutoloading.agent;

import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;

public class NarutoAgent {
    public static void premain(String agentArgs, @NotNull Instrumentation inst) {
        System.out.println("[NarutoAgent] premain loaded successfully.");
        inst.addTransformer(new DisplayWindowTransformer(), false);
    }
}
