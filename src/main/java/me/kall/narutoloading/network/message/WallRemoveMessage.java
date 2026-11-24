package me.kall.narutoloading.network.message;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.network.handler.ClientPacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record WallRemoveMessage(long[] corners) implements CustomPacketPayload {
    public static final Type<WallRemoveMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "wall_remove"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WallRemoveMessage> STREAM_CODEC =
            StreamCodec.of((buffer, message) -> buffer.writeLongArray(message.corners()), buffer -> new WallRemoveMessage(buffer.readLongArray()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WallRemoveMessage message, @NotNull IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandler.remove(message.corners()));
    }
}
