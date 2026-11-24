package me.kall.narutoloading.bootstrap;

import me.kall.narutoloading.client.ui.NarutoScreen;
import net.neoforged.fml.ModContainer;

public final class ClientSetup {
    private ClientSetup() {}

    public static void register(ModContainer context) {
        NarutoScreen.register(context);
    }
}
