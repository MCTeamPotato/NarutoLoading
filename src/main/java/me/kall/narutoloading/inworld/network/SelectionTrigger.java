package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.data.ClientScreens;
import me.kall.narutoloading.inworld.gui.InWorldSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SelectionTrigger {
    private final long position;

    public SelectionTrigger(long position) {
        this.position = position;
    }

    public SelectionTrigger(@NotNull FriendlyByteBuf buf) {
        this.position = buf.readLong();
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLong(this.position);
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            ClientLevel level = minecraft.level;
            if (player == null || level == null) return;
            ObjectSet<ClientScreens.ClientScreen> clientScreens = ClientScreens.CLIENT_SCREENS.get(level.dimension().location());
            for (ClientScreens.ClientScreen clientScreen : clientScreens) {
                if (clientScreen.screen().involved().contains(this.position)) {
                    minecraft.setScreen(new InWorldSelectionScreen(minecraft.screen, clientScreen));
                    NarutoLoading.LOGGER.info("Screen");
                    break;
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
