package me.kall.narutoloading.wall.client.compat;

import me.kall.narutoloading.bootstrap.Integration;

public final class ShaderPack {
    private ShaderPack() {}

    public static boolean inUse() {
        return Integration.IRIS && IrisShaderPack.inUse();
    }
}
