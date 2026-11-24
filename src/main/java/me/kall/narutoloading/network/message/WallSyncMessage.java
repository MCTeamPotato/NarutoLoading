package me.kall.narutoloading.network.message;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.network.handler.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record WallSyncMessage(@NotNull ListTag walls) implements CustomPacketPayload {
    private static final String WALLS_KEY = "Walls";

    public static final Type<WallSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "wall_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WallSyncMessage> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, WallSyncMessage::toTag, WallSyncMessage::fromTag);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WallSyncMessage message, @NotNull IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandler.sync(message.walls()));
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(WALLS_KEY, this.walls);
        return tag;
    }

    private static WallSyncMessage fromTag(CompoundTag tag) {
        return new WallSyncMessage(tag.getList(WALLS_KEY, Tag.TAG_COMPOUND));
    }
}
