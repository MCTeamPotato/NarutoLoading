package me.kall.narutoloading.wall.client.compat;

import me.kall.narutoloading.bootstrap.Integration;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class ShaderPack {
    private static final String IRIS_API = "net.irisshaders.iris.api.v0.IrisApi";

    private static final boolean PRESENT = Integration.modLoaded("oculus") || Integration.modLoaded("iris");

    private static @Nullable Method instanceMethod;
    private static @Nullable Method inUseMethod;
    private static @Nullable Object instance;
    private static boolean failed;

    private ShaderPack() {}

    public static boolean inUse() {
        if (!PRESENT || failed) return false;

        try {
            if (inUseMethod == null) {
                Class<?> api = Class.forName(IRIS_API);
                instanceMethod = api.getMethod("getInstance");
                inUseMethod = api.getMethod("isShaderPackInUse");
            }

            if (instance == null) instance = instanceMethod.invoke(null);
            if (instance == null) return false;

            Object value = inUseMethod.invoke(instance);
            return value instanceof Boolean used && used;
        } catch (Throwable throwable) {
            failed = true;
            instanceMethod = null;
            inUseMethod = null;
            instance = null;
            System.err.println("[NarutoLoading] Could not query the Iris API, assuming no shader pack is in use: " + throwable.getMessage());
            return false;
        }
    }
}
