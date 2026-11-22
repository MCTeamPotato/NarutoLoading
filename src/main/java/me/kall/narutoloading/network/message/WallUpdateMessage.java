package me.kall.narutoloading.network.message;

import me.kall.narutoloading.network.ServerPacketHandler;
import me.kall.narutoloading.wall.Wall;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class WallUpdateMessage {
    private static final int MEDIA_LIMIT = 256;

    private final long[] corners;
    private final String media;
    private final float volume;
    private final boolean light;

    public WallUpdateMessage(@NotNull Wall wall) {
        this.corners = wall.toLongArray();
        this.media = wall.media;
        this.volume = wall.volume;
        this.light = wall.light;
    }

    private WallUpdateMessage(long[] corners, String media, float volume, boolean light) {
        this.corners = corners;
        this.media = media;
        this.volume = volume;
        this.light = light;
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeLongArray(this.corners);
        buffer.writeUtf(this.media, MEDIA_LIMIT);
        buffer.writeFloat(this.volume);
        buffer.writeBoolean(this.light);
    }

    public static @NotNull WallUpdateMessage decode(@NotNull FriendlyByteBuf buffer) {
        return new WallUpdateMessage(buffer.readLongArray(), buffer.readUtf(MEDIA_LIMIT), buffer.readFloat(), buffer.readBoolean());
    }

    public static void handle(WallUpdateMessage message, @NotNull Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ServerPacketHandler.update(context.getSender(), message.corners, message.media, message.volume, message.light));
        context.setPacketHandled(true);
    }
}
