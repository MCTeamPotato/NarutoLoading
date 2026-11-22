package me.kall.narutoloading.network.message;

import me.kall.narutoloading.network.handler.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class WallRemoveMessage {
    private final long[] corners;

    public WallRemoveMessage(long @NotNull [] corners) {
        this.corners = corners;
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeLongArray(this.corners);
    }

    public static @NotNull WallRemoveMessage decode(@NotNull FriendlyByteBuf buffer) {
        return new WallRemoveMessage(buffer.readLongArray());
    }

    public static void handle(WallRemoveMessage message, @NotNull Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPacketHandler.remove(message.corners));
        context.setPacketHandled(true);
    }
}
