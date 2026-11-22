package me.kall.narutoloading;

import me.kall.narutoloading.bootstrap.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";

    public NarutoLoading(FMLJavaModLoadingContext context) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> ClientSetup.register(context));
    }
}
