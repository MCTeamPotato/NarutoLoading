package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.data.ClientScreens;
import me.kall.narutoloading.inworld.gui.InWorldSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SourceSelectionPacket {
    private final long position;

    public SourceSelectionPacket(long position) {
        this.position = position;
    }

    public SourceSelectionPacket(@NotNull FriendlyByteBuf buf) {
        this.position = buf.readLong();
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLong(this.position);
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                Minecraft minecraft = Minecraft.getInstance();
                LocalPlayer player = minecraft.player;
                ClientLevel level = minecraft.level;
                if (player == null || level == null) return;
                ResourceLocation dimension = level.dimension().location();
                ObjectSet<ClientScreens.ClientScreen> clientScreens = ClientScreens.CLIENT_SCREENS.get(dimension);
                if (clientScreens == null) return;
                for (ClientScreens.ClientScreen clientScreen : clientScreens) {
                    if (clientScreen.screen().involved().contains(this.position)) {
                        minecraft.setScreen(new InWorldSelectionScreen(minecraft.screen, clientScreen));
                        break;
                    }
                }
            } catch (Exception exception) {
                NarutoLoading.LOGGER.error("Error handling SourceSelectionPacket", exception);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
