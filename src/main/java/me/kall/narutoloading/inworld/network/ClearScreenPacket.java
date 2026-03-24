package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.ext.ScreenLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ClearScreenPacket {
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

    public void handle(@NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    ServerLevel level = player.serverLevel();
                    this.areaInvolved.removeIf(this.borderInvolved::contains);
                    LongIterator positionsToRemove = this.areaInvolved.longIterator();

                    Component start = Component.translatable("info.narutoloading.clear.begin", this.screenInfo);

                    for (ServerPlayer online : level.players()) {
                        online.displayClientMessage(start, false);
                    }

                    ((ScreenLevel)level).naruto$setClearingScreens(true);

                    while (positionsToRemove.hasNext()) {
                        long next = positionsToRemove.nextLong();
                        if (Displayers.isDisplayer(level, next)) {
                            level.removeBlock(BlockPos.of(next), false);
                        }
                    }

                    ((ScreenLevel)level).naruto$setClearingScreens(false);

                    Component end = Component.translatable("info.narutoloading.clear.end", this.screenInfo);

                    for (ServerPlayer online : level.players()) {
                        online.displayClientMessage(end, false);
                    }
                }
            } catch (Exception ignored) {}
        });
        ctx.get().setPacketHandled(true);
    }
}
