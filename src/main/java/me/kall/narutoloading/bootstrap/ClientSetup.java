package me.kall.narutoloading.bootstrap;

import me.kall.narutoloading.client.ui.NarutoScreen;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ClientSetup {
    private ClientSetup() {}

    public static void register(FMLJavaModLoadingContext context) {
        if (Integration.NARUTO_UI) NarutoScreen.register(context);
    }
}
