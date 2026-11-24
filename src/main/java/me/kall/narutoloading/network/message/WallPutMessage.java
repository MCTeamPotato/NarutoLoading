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

public record WallPutMessage(@NotNull CompoundTag wall) implements CustomPacketPayload {
    public static final Type<WallPutMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "wall_put"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WallPutMessage> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, WallPutMessage::wall, WallPutMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WallPutMessage message, @NotNull IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandler.put(message.wall()));
    }
}
