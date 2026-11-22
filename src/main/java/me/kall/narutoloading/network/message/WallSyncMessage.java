package me.kall.narutoloading.network.message;

import me.kall.narutoloading.network.handler.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class WallSyncMessage {
    private static final String WALLS_KEY = "Walls";

    private final ListTag walls;

    public WallSyncMessage(@NotNull ListTag walls) {
        this.walls = walls;
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        CompoundTag tag = new CompoundTag();
        tag.put(WALLS_KEY, this.walls);
        buffer.writeNbt(tag);
    }

    public static @NotNull WallSyncMessage decode(@NotNull FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new WallSyncMessage(tag == null ? new ListTag() : tag.getList(WALLS_KEY, Tag.TAG_COMPOUND));
    }

    public static void handle(WallSyncMessage message, @NotNull Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandler.sync(message.walls));
        context.setPacketHandled(true);
    }
}
