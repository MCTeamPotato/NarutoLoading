package me.kall.narutoloading.inworld.init;

import me.kall.duplicationless.network.Networker;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.network.ScreenDelivery;
import me.kall.narutoloading.inworld.network.SelectionTrigger;
import net.minecraftforge.network.simple.SimpleChannel;

public class NarutoPackets {
    public static final SimpleChannel INSTANCE = Networker.create(NarutoLoading.MOD_ID, "1");
    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, ScreenDelivery.class, ScreenDelivery::encode, ScreenDelivery::new, ScreenDelivery::handle);
        INSTANCE.registerMessage(id++, SelectionTrigger.class, SelectionTrigger::encode, SelectionTrigger::new, SelectionTrigger::handle);
    }
}
