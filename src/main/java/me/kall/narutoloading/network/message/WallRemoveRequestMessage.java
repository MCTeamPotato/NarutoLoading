package me.kall.narutoloading.network.message;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.network.handler.ServerPacketHandler;
import me.kall.narutoloading.wall.Wall;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record WallRemoveRequestMessage(long[] corners) implements CustomPacketPayload {
    public static final Type<WallRemoveRequestMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "wall_remove_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WallRemoveRequestMessage> STREAM_CODEC =
            StreamCodec.of((buffer, message) -> buffer.writeLongArray(message.corners()), buffer -> new WallRemoveRequestMessage(buffer.readLongArray()));

    public WallRemoveRequestMessage(@NotNull Wall wall) {
        this(wall.toLongArray());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WallRemoveRequestMessage message, @NotNull IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerPacketHandler.remove(player, message.corners());
            }
        });
    }
}
