package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.inworld.data.ClientScreens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public class ScreenLifePacket {
    private final InWorldScreen inWorldScreen;
    private final boolean isRemoval;

    public ScreenLifePacket(InWorldScreen inWorldScreen, boolean isRemoval) {
        this.inWorldScreen = inWorldScreen;
        this.isRemoval = isRemoval;
    }

    public ScreenLifePacket(@NotNull FriendlyByteBuf buf) {
        this.inWorldScreen = InWorldScreen.from(buf.readLongArray(), buf.readResourceLocation(), buf.readUtf(), buf.readUtf());
        this.isRemoval = buf.readBoolean();
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLongArray(this.inWorldScreen.toLongArray());
        buf.writeResourceLocation(this.inWorldScreen.dimension());
        buf.writeUtf(this.inWorldScreen.video(""));
        buf.writeUtf(this.inWorldScreen.audio(""));
        buf.writeBoolean(this.isRemoval);
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                if (this.isRemoval) {
                    ObjectSet<ClientScreens.ClientScreen> clientScreens = ClientScreens.CLIENT_SCREENS.get(this.inWorldScreen.dimension());
                    if (clientScreens != null) {
                        ObjectIterator<ClientScreens.ClientScreen> clientScreenIterator = clientScreens.iterator();
                        while (clientScreenIterator.hasNext()) {
                            ClientScreens.ClientScreen clientScreen = clientScreenIterator.next();
                            if (clientScreen.screen().equals(this.inWorldScreen)) {
                                clientScreenIterator.remove();
                                clientScreen.renderer().shutdown();
                                break;
                            }
                        }
                        NarutoLoading.LOGGER.info("Delivered {} for removal.", this.inWorldScreen.toString());
                    }
                } else {
                    Optional.ofNullable(ClientScreens.CLIENT_SCREENS.get(this.inWorldScreen.dimension())).ifPresent(clientScreens -> {
                        ObjectIterator<ClientScreens.ClientScreen> clientScreenIterator = clientScreens.iterator();
                        while (clientScreenIterator.hasNext()) {
                            ClientScreens.ClientScreen clientScreen = clientScreenIterator.next();
                            if (clientScreen.screen().equals(this.inWorldScreen)) {
                                clientScreenIterator.remove();
                                clientScreen.renderer().shutdown();
                                break;
                            }
                        }
                    });
                    ClientScreens.CLIENT_SCREENS.computeIfAbsent(this.inWorldScreen.dimension(), key -> new ObjectOpenHashSet<>()).add(new ClientScreens.ClientScreen(this.inWorldScreen, new NarutoInWorldRenderer(this.inWorldScreen)));
                    NarutoLoading.LOGGER.info("Delivered {} for addition.", this.inWorldScreen.toString());
                }
            } catch (Exception exception) {
                NarutoLoading.LOGGER.error("Error handling ScreenLifePacket", exception);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
