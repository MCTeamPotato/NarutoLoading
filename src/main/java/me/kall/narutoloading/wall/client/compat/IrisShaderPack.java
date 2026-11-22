package me.kall.narutoloading.wall.client.compat;

import net.irisshaders.iris.api.v0.IrisApi;

final class IrisShaderPack {
    private static boolean failed;

    private IrisShaderPack() {}

    static boolean inUse() {
        if (failed) return false;

        try {
            return IrisApi.getInstance().isShaderPackInUse();
        } catch (Throwable throwable) {
            failed = true;
            return false;
        }
    }
}
