package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.inworld.gui.InWorldSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class SourceSelectionPacket implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, SourceSelectionPacket> CODEC = CustomPacketPayload.codec(SourceSelectionPacket::encode, SourceSelectionPacket::new);
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "source_selection");
    public static final CustomPacketPayload.Type<SourceSelectionPacket> TYPE = new CustomPacketPayload.Type<>(ID);

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

    public static void handle(SourceSelectionPacket packet, @NotNull IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                Minecraft minecraft = Minecraft.getInstance();
                LocalPlayer player = minecraft.player;
                ClientLevel level = minecraft.level;
                if (player == null || level == null) return;
                ResourceLocation dimension = level.dimension().location();
                ObjectSet<NarutoInWorldRenderer> renderers = ClientScreensRenderer.CLIENT_SCREENS.get(dimension);
                if (renderers == null) return;
                for (NarutoInWorldRenderer renderer : renderers) {
                    if (renderer.screen.borderInvolved().contains(packet.position)) {
                        minecraft.setScreen(new InWorldSelectionScreen(minecraft.screen, renderer));
                        break;
                    }
                }
            } catch (Exception exception) {
                NarutoLoading.LOGGER.error("Error handling SourceSelectionPacket", exception);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
