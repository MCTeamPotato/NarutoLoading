package me.kall.narutoloading;

import me.kall.narutoloading.bootstrap.ClientSetup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";

    public NarutoLoading(FMLJavaModLoadingContext context) {
        if (FMLLoader.getDist().isClient()) ClientSetup.register(context);
    }
}
