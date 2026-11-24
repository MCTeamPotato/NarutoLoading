package me.kall.narutoloading.network.message;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.network.handler.ServerPacketHandler;
import me.kall.narutoloading.wall.Wall;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record WallUpdateMessage(long[] corners, String media, float volume, boolean light) implements CustomPacketPayload {
    private static final int MEDIA_LIMIT = 256;

    public static final Type<WallUpdateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "wall_update"));

    private static final StreamCodec<RegistryFriendlyByteBuf, long[]> CORNERS =
            StreamCodec.of((buffer, corners) -> buffer.writeLongArray(corners), FriendlyByteBuf::readLongArray);

    public static final StreamCodec<RegistryFriendlyByteBuf, WallUpdateMessage> STREAM_CODEC = StreamCodec.composite(
            CORNERS, WallUpdateMessage::corners,
            ByteBufCodecs.stringUtf8(MEDIA_LIMIT), WallUpdateMessage::media,
            ByteBufCodecs.FLOAT, WallUpdateMessage::volume,
            ByteBufCodecs.BOOL, WallUpdateMessage::light,
            WallUpdateMessage::new);

    public WallUpdateMessage(@NotNull Wall wall) {
        this(wall.toLongArray(), wall.media, wall.volume, wall.light);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WallUpdateMessage message, @NotNull IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerPacketHandler.update(player, message.corners(), message.media(), message.volume(), message.light());
            }
        });
    }
}
