package me.kall.narutoloading.network;

import me.kall.narutoloading.network.message.OpenWallConfigMessage;
import me.kall.narutoloading.network.message.WallCleanRequestMessage;
import me.kall.narutoloading.network.message.WallPutMessage;
import me.kall.narutoloading.network.message.WallRemoveMessage;
import me.kall.narutoloading.network.message.WallRemoveRequestMessage;
import me.kall.narutoloading.network.message.WallSyncMessage;
import me.kall.narutoloading.network.message.WallUpdateMessage;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

public final class NarutoNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private NarutoNetwork() {}

    public static void register(@NotNull RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(WallSyncMessage.TYPE, WallSyncMessage.STREAM_CODEC, WallSyncMessage::handle);
        registrar.playToClient(WallPutMessage.TYPE, WallPutMessage.STREAM_CODEC, WallPutMessage::handle);
        registrar.playToClient(WallRemoveMessage.TYPE, WallRemoveMessage.STREAM_CODEC, WallRemoveMessage::handle);
        registrar.playToClient(OpenWallConfigMessage.TYPE, OpenWallConfigMessage.STREAM_CODEC, OpenWallConfigMessage::handle);

        registrar.playToServer(WallUpdateMessage.TYPE, WallUpdateMessage.STREAM_CODEC, WallUpdateMessage::handle);
        registrar.playToServer(WallRemoveRequestMessage.TYPE, WallRemoveRequestMessage.STREAM_CODEC, WallRemoveRequestMessage::handle);
        registrar.playToServer(WallCleanRequestMessage.TYPE, WallCleanRequestMessage.STREAM_CODEC, WallCleanRequestMessage::handle);
    }
}
