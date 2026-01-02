package me.kall.narutoloading.inworld.init;

import me.kall.duplicationless.network.Networker;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.network.ArgUpdatePacket;
import me.kall.narutoloading.inworld.network.ClearScreenPacket;
import me.kall.narutoloading.inworld.network.ScreenLifePacket;
import me.kall.narutoloading.inworld.network.SourceSelectionPacket;
import net.minecraftforge.network.simple.SimpleChannel;

public class NarutoPackets {
    public static final SimpleChannel INSTANCE = Networker.create(NarutoLoading.MOD_ID, "1");
    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, ScreenLifePacket.class, ScreenLifePacket::encode, ScreenLifePacket::new, ScreenLifePacket::handle);
        INSTANCE.registerMessage(id++, SourceSelectionPacket.class, SourceSelectionPacket::encode, SourceSelectionPacket::new, SourceSelectionPacket::handle);
        INSTANCE.registerMessage(id++, ArgUpdatePacket.class, ArgUpdatePacket::encode, ArgUpdatePacket::new, ArgUpdatePacket::handle);
        INSTANCE.registerMessage(id++, ClearScreenPacket.class, ClearScreenPacket::encode, ClearScreenPacket::new, ClearScreenPacket::handle);
    }
}
