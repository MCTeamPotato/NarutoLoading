package me.kall.narutoloading.network.message;

import me.kall.narutoloading.network.ServerPacketHandler;
import me.kall.narutoloading.world.Wall;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class WallCleanRequestMessage {
    private final long[] corners;

    public WallCleanRequestMessage(@NotNull Wall wall) {
        this.corners = wall.toLongArray();
    }

    private WallCleanRequestMessage(long[] corners) {
        this.corners = corners;
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeLongArray(this.corners);
    }

    public static @NotNull WallCleanRequestMessage decode(@NotNull FriendlyByteBuf buffer) {
        return new WallCleanRequestMessage(buffer.readLongArray());
    }

    public static void handle(WallCleanRequestMessage message, @NotNull Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ServerPacketHandler.clean(context.getSender(), message.corners));
        context.setPacketHandled(true);
    }
}
