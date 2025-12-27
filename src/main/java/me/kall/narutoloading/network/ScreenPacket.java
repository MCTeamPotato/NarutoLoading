package me.kall.narutoloading.network;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.NarutoInWorldRenderer;
import me.kall.narutoloading.core.detection.inworld.InWorldScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ScreenPacket {
    private final InWorldScreen inWorldScreen;
    private final boolean isRemoval;

    public ScreenPacket(InWorldScreen inWorldScreen, boolean isRemoval) {
        this.inWorldScreen = inWorldScreen;
        this.isRemoval = isRemoval;
    }

    public ScreenPacket(@NotNull FriendlyByteBuf buf) {
        this.inWorldScreen = InWorldScreen.from(buf.readLongArray(), buf.readResourceLocation(), buf.readUtf(), buf.readUtf());
        this.isRemoval = buf.readBoolean();
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLongArray(this.inWorldScreen.toLongArray());
        buf.writeResourceLocation(this.inWorldScreen.dimension());
        buf.writeUtf(this.inWorldScreen.video);
        buf.writeUtf(this.inWorldScreen.audio);
        buf.writeBoolean(this.isRemoval);
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!this.isRemoval) {
                NarutoInWorldRenderer.INSTANCE.screens.computeIfAbsent(this.inWorldScreen.dimension(), key -> new ObjectOpenHashSet<>()).add(this.inWorldScreen);
                NarutoLoading.LOGGER.info("Delivered {} for addition.", this.inWorldScreen.toString());
            } else {
                ObjectSet<InWorldScreen> inWorldScreens = NarutoInWorldRenderer.INSTANCE.screens.get(this.inWorldScreen.dimension());
                if (inWorldScreens != null) {
                    inWorldScreens.remove(this.inWorldScreen);
                    NarutoLoading.LOGGER.info("Delivered {} for removal.", this.inWorldScreen.toString());
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
