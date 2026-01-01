package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.data.Screens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ArgUpdatePacket {
    private final InWorldScreen argSource;

    public ArgUpdatePacket(InWorldScreen argSource) {
        this.argSource = argSource;
    }

    public ArgUpdatePacket(@NotNull FriendlyByteBuf buf) {
        this.argSource = InWorldScreen.from(buf.readLongArray(), buf.readResourceLocation(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readResourceLocation(), buf.readBoolean());
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLongArray(this.argSource.toLongArray());
        buf.writeResourceLocation(this.argSource.dimension());
        buf.writeUtf(this.argSource.relativeVideoPath(""));
        buf.writeUtf(this.argSource.relativeAudioPath(""));
        buf.writeBoolean(this.argSource.isCullable());
        buf.writeResourceLocation(this.argSource.getLocalSound());
        buf.writeBoolean(this.argSource.hideInner());
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                ServerLevel level = player.serverLevel();
                Screens screens = Screens.get(level);

                ObjectSet<InWorldScreen> inWorldScreens = screens.screens.computeIfAbsent(level.dimension().location(), key -> new ObjectOpenHashSet<>());

                inWorldScreens.remove(this.argSource);
                inWorldScreens.add(this.argSource);

                screens.setDirty();
                NarutoLoading.LOGGER.info("{}Successfully sync the video and audio arguments for {}.", NarutoLoading.info(), this.argSource.toString());
            } catch (Exception exception) {
                NarutoLoading.LOGGER.error("Error handling ArgUpdatePacket", exception);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
