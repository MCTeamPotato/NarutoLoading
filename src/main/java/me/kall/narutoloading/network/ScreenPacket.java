package me.kall.narutoloading.network;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.narutoloading.core.NarutoInWorldRenderer;
import me.kall.narutoloading.core.detection.inworld.ScreenChecker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ScreenPacket {
    private final ScreenChecker.Screen screen;

    public ScreenPacket(ScreenChecker.Screen screen) {
        this.screen = screen;
    }

    public ScreenPacket(@NotNull FriendlyByteBuf buf) {
        this.screen = ScreenChecker.Screen.from(buf.readLongArray(), buf.readResourceLocation());
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLongArray(this.screen.toLongArray());
        buf.writeResourceLocation(this.screen.dimension());
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> NarutoInWorldRenderer.SCREENS.computeIfAbsent(this.screen.dimension(), key -> new ObjectOpenHashSet<>()).add(this.screen));
        ctx.get().setPacketHandled(true);
    }
}
