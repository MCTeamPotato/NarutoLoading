package me.kall.narutoloading.inworld.init;

import me.kall.narutoloading.inworld.network.ArgUpdatePacket;
import me.kall.narutoloading.inworld.network.ClearScreenPacket;
import me.kall.narutoloading.inworld.network.ScreenLifePacket;
import me.kall.narutoloading.inworld.network.SourceSelectionPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

public class NarutoPackets {
    public static void register(@NotNull RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(ArgUpdatePacket.TYPE, ArgUpdatePacket.CODEC, ArgUpdatePacket::handle);
        registrar.playToClient(ClearScreenPacket.TYPE, ClearScreenPacket.CODEC, ClearScreenPacket::handle);
        registrar.playToClient(ScreenLifePacket.TYPE, ScreenLifePacket.CODEC, ScreenLifePacket::handle);
        registrar.playToClient(SourceSelectionPacket.TYPE, SourceSelectionPacket.CODEC, SourceSelectionPacket::handle);
    }
}
