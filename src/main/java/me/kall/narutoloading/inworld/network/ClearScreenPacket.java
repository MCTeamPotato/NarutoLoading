package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.data.Displayers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ClearScreenPacket implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, ClearScreenPacket> CODEC = CustomPacketPayload.codec(ClearScreenPacket::encode, ClearScreenPacket::new);
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "clear_screen");
    public static final CustomPacketPayload.Type<ClearScreenPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private final LongSet areaInvolved;
    private final LongSet borderInvolved;
    private final String screenInfo;

    public ClearScreenPacket(@NotNull InWorldScreen screen) {
        this.areaInvolved = new LongOpenHashSet(screen.areaInvolved().toLongArray());
        this.borderInvolved = new LongOpenHashSet(screen.borderInvolved().toLongArray());
        this.screenInfo = screen.toLocalString();
    }

    public ClearScreenPacket(@NotNull FriendlyByteBuf buf) {
        this.areaInvolved = new LongOpenHashSet(buf.readLongArray());
        this.borderInvolved = new LongOpenHashSet(buf.readLongArray());
        this.screenInfo = buf.readUtf();
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLongArray(this.areaInvolved.toLongArray());
        buf.writeLongArray(this.borderInvolved.toLongArray());
        buf.writeUtf(this.screenInfo);
    }

    public static void handle(ClearScreenPacket packet, @NotNull IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                Player player = ctx.player();
                if (player instanceof ServerPlayer) {
                    ServerLevel level = ((ServerPlayer) player).serverLevel();
                    packet.areaInvolved.removeIf(packet.borderInvolved::contains);
                    LongIterator positionsToRemove = packet.areaInvolved.longIterator();

                    Component start = Component.translatable("info.narutoloading.clear.begin", packet.screenInfo);

                    for (ServerPlayer online : level.players()) {
                        online.displayClientMessage(start, false);
                    }

                    while (positionsToRemove.hasNext()) {
                        long next = positionsToRemove.nextLong();
                        if (Displayers.isDisplayer(level, next)) {
                            level.removeBlock(BlockPos.of(next), false);
                        }
                    }

                    Component end = Component.translatable("info.narutoloading.clear.end", packet.screenInfo);

                    for (ServerPlayer online : level.players()) {
                        online.displayClientMessage(end, false);
                    }
                }
            } catch (Exception exception) {
                NarutoLoading.LOGGER.error("Error handling ClearScreenPacket", exception);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
