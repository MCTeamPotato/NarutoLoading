package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.data.Screens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ArgUpdatePacket implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, ArgUpdatePacket> CODEC = CustomPacketPayload.codec(ArgUpdatePacket::encode, ArgUpdatePacket::new);
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "arg_update");
    public static final Type<ArgUpdatePacket> TYPE = new Type<>(ID);

    private final InWorldScreen argSource;

    public ArgUpdatePacket(InWorldScreen argSource) {
        this.argSource = argSource;
    }

    public ArgUpdatePacket(@NotNull FriendlyByteBuf buf) {
        this.argSource = InWorldScreen.from(buf.readLongArray(), buf.readResourceLocation(), buf.readUtf(), buf.readUtf(), buf.readResourceLocation(), buf.readFloat(), buf.readBoolean(), buf.readInt(), buf.readInt());
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLongArray(this.argSource.toLongArray());
        buf.writeResourceLocation(this.argSource.dimension());
        buf.writeUtf(this.argSource.relativeVideoPath(NarutoLoading.BLANK));
        buf.writeUtf(this.argSource.relativeAudioPath(NarutoLoading.BLANK));
        buf.writeResourceLocation(this.argSource.localSound());
        buf.writeFloat(this.argSource.soundVolume());
        buf.writeBoolean(this.argSource.hideInner());
        buf.writeInt(this.argSource.videoWidth());
        buf.writeInt(this.argSource.videoHeight());
    }

    public static void handle(ArgUpdatePacket packet, @NotNull IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                Player player = ctx.player();
                if (!(player instanceof ServerPlayer)) return;
                ServerLevel level = ((ServerPlayer) player).serverLevel();
                Screens screens = Screens.get(level);

                ObjectSet<InWorldScreen> inWorldScreens = screens.screens.computeIfAbsent(level.dimension().location(), key -> new ObjectOpenHashSet<>());

                inWorldScreens.remove(packet.argSource);
                inWorldScreens.add(packet.argSource);

                screens.setDirty();
                NarutoLoading.LOGGER.info("{}Successfully sync the video and audio arguments for {}.", NarutoLoading.info(), packet.argSource.toString());
            } catch (Exception exception) {
                NarutoLoading.LOGGER.error("Error handling ArgUpdatePacket", exception);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
