package me.kall.narutoloading.network.message;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.network.handler.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record OpenWallConfigMessage(@NotNull CompoundTag wall) implements CustomPacketPayload {
    public static final Type<OpenWallConfigMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "open_wall_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenWallConfigMessage> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, OpenWallConfigMessage::wall, OpenWallConfigMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenWallConfigMessage message, @NotNull IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandler.openConfig(message.wall()));
    }
}
