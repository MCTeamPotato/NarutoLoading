package me.kall.narutoloading.client.context;

import net.minecraft.client.Minecraft;

public class LevelStatus {
    public static boolean notFound() {
        return Minecraft.getInstance().level == null;
    }
}
