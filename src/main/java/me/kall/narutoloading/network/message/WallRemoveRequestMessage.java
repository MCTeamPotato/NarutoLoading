package me.kall.narutoloading.network.message;

import me.kall.narutoloading.network.handler.ServerPacketHandler;
import me.kall.narutoloading.wall.Wall;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class WallRemoveRequestMessage {
    private final long[] corners;

    public WallRemoveRequestMessage(@NotNull Wall wall) {
        this.corners = wall.toLongArray();
    }

    private WallRemoveRequestMessage(long[] corners) {
        this.corners = corners;
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeLongArray(this.corners);
    }

    public static @NotNull WallRemoveRequestMessage decode(@NotNull FriendlyByteBuf buffer) {
        return new WallRemoveRequestMessage(buffer.readLongArray());
    }

    public static void handle(WallRemoveRequestMessage message, @NotNull Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ServerPacketHandler.remove(context.getSender(), message.corners));
        context.setPacketHandled(true);
    }
}
