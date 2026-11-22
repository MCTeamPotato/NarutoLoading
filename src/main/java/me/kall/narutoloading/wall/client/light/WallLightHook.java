package me.kall.narutoloading.wall.client.light;

public final class WallLightHook {
    private static volatile boolean active;

    private WallLightHook() {}

    public static boolean active() {
        return active;
    }

    public static void active(boolean value) {
        active = value;
    }
}
