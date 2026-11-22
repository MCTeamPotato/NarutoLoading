package me.kall.narutoloading.network.message;

import me.kall.narutoloading.network.handler.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class WallPutMessage {
    private final @Nullable CompoundTag wall;

    public WallPutMessage(@Nullable CompoundTag wall) {
        this.wall = wall;
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeNbt(this.wall);
    }

    public static @NotNull WallPutMessage decode(@NotNull FriendlyByteBuf buffer) {
        return new WallPutMessage(buffer.readNbt());
    }

    public static void handle(WallPutMessage message, @NotNull Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandler.put(message.wall));
        context.setPacketHandled(true);
    }
}
