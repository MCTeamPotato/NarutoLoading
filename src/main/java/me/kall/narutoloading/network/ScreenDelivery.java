package me.kall.narutoloading.network;

import me.kall.duplicationless.network.Networker;
import me.kall.narutoloading.NarutoLoading;
import net.minecraftforge.network.simple.SimpleChannel;

public class ScreenDelivery {
    public static final SimpleChannel INSTANCE = Networker.create(NarutoLoading.MOD_ID, "1");
    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, ScreenPacket.class, ScreenPacket::encode, ScreenPacket::new, ScreenPacket::handle);
    }
}
