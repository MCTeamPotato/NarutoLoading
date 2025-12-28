package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.duplicationless.network.Networker;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.inworld.data.ClientScreens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ScreenDelivery {
    public static final SimpleChannel INSTANCE = Networker.create(NarutoLoading.MOD_ID, "1");

    private static int id = 0;

    private final InWorldScreen inWorldScreen;
    private final boolean isRemoval;

    public ScreenDelivery(InWorldScreen inWorldScreen, boolean isRemoval) {
        this.inWorldScreen = inWorldScreen;
        this.isRemoval = isRemoval;
    }

    public ScreenDelivery(@NotNull FriendlyByteBuf buf) {
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
                ClientScreens.CLIENT_SCREENS.computeIfAbsent(this.inWorldScreen.dimension(), key -> new ObjectOpenHashSet<>()).add(new ClientScreens.ClientScreen(this.inWorldScreen, new NarutoInWorldRenderer(this.inWorldScreen)));
                NarutoLoading.LOGGER.info("Delivered {} for addition.", this.inWorldScreen.toString());
            } else {
                ObjectSet<ClientScreens.ClientScreen> inWorldScreens = ClientScreens.CLIENT_SCREENS.get(this.inWorldScreen.dimension());
                if (inWorldScreens != null) {
                    inWorldScreens.removeIf(clientScreen -> clientScreen.screen().equals(this.inWorldScreen));
                    NarutoLoading.LOGGER.info("Delivered {} for removal.", this.inWorldScreen.toString());
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void register() {
        INSTANCE.registerMessage(id++, ScreenDelivery.class, ScreenDelivery::encode, ScreenDelivery::new, ScreenDelivery::handle);
    }
}
