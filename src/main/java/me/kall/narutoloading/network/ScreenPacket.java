package me.kall.narutoloading.network;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoInWorldRenderer;
import me.kall.narutoloading.core.detection.inworld.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ScreenPacket {
    private final Screen screen;
    private final boolean isRemoval;

    public ScreenPacket(Screen screen, boolean isRemoval) {
        this.screen = screen;
        this.isRemoval = isRemoval;
    }

    public ScreenPacket(@NotNull FriendlyByteBuf buf) {
        this.screen = Screen.from(buf.readLongArray(), buf.readResourceLocation());
        this.isRemoval = buf.readBoolean();
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLongArray(this.screen.toLongArray());
        buf.writeResourceLocation(this.screen.dimension());
        buf.writeBoolean(this.isRemoval);
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!this.isRemoval) {
                NarutoInWorldRenderer.INSTANCE.screens.computeIfAbsent(this.screen.dimension(), key -> new ObjectOpenHashSet<>()).add(this.screen);
                NarutoLoading.LOGGER.info("Delivered {}", this.screen.toString());
            } else {
                ObjectSet<Screen> screens = NarutoInWorldRenderer.INSTANCE.screens.get(this.screen.dimension());
                if (screens != null) screens.remove(this.screen);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
