package me.kall.narutoloading.network;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.network.message.OpenWallConfigMessage;
import me.kall.narutoloading.network.message.WallCleanRequestMessage;
import me.kall.narutoloading.network.message.WallPutMessage;
import me.kall.narutoloading.network.message.WallRemoveMessage;
import me.kall.narutoloading.network.message.WallRemoveRequestMessage;
import me.kall.narutoloading.network.message.WallSyncMessage;
import me.kall.narutoloading.network.message.WallUpdateMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NarutoNetwork {
    public static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    private static int messageId;

    private NarutoNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(messageId++, WallSyncMessage.class, WallSyncMessage::encode, WallSyncMessage::decode, WallSyncMessage::handle);
        CHANNEL.registerMessage(messageId++, WallPutMessage.class, WallPutMessage::encode, WallPutMessage::decode, WallPutMessage::handle);
        CHANNEL.registerMessage(messageId++, WallRemoveMessage.class, WallRemoveMessage::encode, WallRemoveMessage::decode, WallRemoveMessage::handle);
        CHANNEL.registerMessage(messageId++, OpenWallConfigMessage.class, OpenWallConfigMessage::encode, OpenWallConfigMessage::decode, OpenWallConfigMessage::handle);

        CHANNEL.registerMessage(messageId++, WallUpdateMessage.class, WallUpdateMessage::encode, WallUpdateMessage::decode, WallUpdateMessage::handle);
        CHANNEL.registerMessage(messageId++, WallRemoveRequestMessage.class, WallRemoveRequestMessage::encode, WallRemoveRequestMessage::decode, WallRemoveRequestMessage::handle);
        CHANNEL.registerMessage(messageId++, WallCleanRequestMessage.class, WallCleanRequestMessage::encode, WallCleanRequestMessage::decode, WallCleanRequestMessage::handle);
    }
}
