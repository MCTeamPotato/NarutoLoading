package me.kall.narutoloading.network;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoInWorldRenderer;
import me.kall.narutoloading.core.detection.inworld.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ScreenPacket {
    private final Screen screen;

    public ScreenPacket(Screen screen) {
        this.screen = screen;
    }

    public ScreenPacket(@NotNull FriendlyByteBuf buf) {
        this.screen = Screen.from(buf.readLongArray(), buf.readResourceLocation());
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLongArray(this.screen.toLongArray());
        buf.writeResourceLocation(this.screen.dimension());
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            NarutoInWorldRenderer.INSTANCE.screens.computeIfAbsent(this.screen.dimension(), key -> new ObjectOpenHashSet<>()).add(this.screen);
            NarutoLoading.LOGGER.info("Delivered {}", this.screen.toString());
        });
        ctx.get().setPacketHandled(true);
    }
}
