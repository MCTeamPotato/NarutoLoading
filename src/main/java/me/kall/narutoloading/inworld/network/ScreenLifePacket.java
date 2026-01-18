package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.config.NarutoConfig;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.inworld.data.HiddenDisplayers;
import me.kall.narutoloading.inworld.gui.util.AudioConverter;
import me.kall.narutoloading.inworld.gui.util.ResourceZipGenerator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Optional;

public class ScreenLifePacket implements CustomPacketPayload{
    public static final StreamCodec<FriendlyByteBuf, ScreenLifePacket> CODEC = CustomPacketPayload.codec(ScreenLifePacket::encode, ScreenLifePacket::new);
    public static final Identifier ID = Identifier.fromNamespaceAndPath(NarutoLoading.MOD_ID, "screen_life");
    public static final CustomPacketPayload.Type<@NotNull ScreenLifePacket> TYPE = new CustomPacketPayload.Type<>(ID);

    private final InWorldScreen inWorldScreen;
    private final boolean isRemoval;

    public ScreenLifePacket(InWorldScreen inWorldScreen, boolean isRemoval) {
        this.inWorldScreen = inWorldScreen;
        this.isRemoval = isRemoval;
    }

    public ScreenLifePacket(@NotNull FriendlyByteBuf buf) {
        this.inWorldScreen = InWorldScreen.from(buf.readLongArray(), buf.readIdentifier(), buf.readUtf(), buf.readUtf(), buf.readIdentifier(), buf.readFloat(), buf.readBoolean(), buf.readInt(), buf.readInt());
        this.isRemoval = buf.readBoolean();
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLongArray(this.inWorldScreen.toLongArray());
        buf.writeIdentifier(this.inWorldScreen.dimension());
        buf.writeUtf(this.inWorldScreen.relativeVideoPath(NarutoLoading.BLANK));
        buf.writeUtf(this.inWorldScreen.relativeAudioPath(NarutoLoading.BLANK));
        buf.writeIdentifier(this.inWorldScreen.localSound());
        buf.writeFloat(this.inWorldScreen.soundVolume());
        buf.writeBoolean(this.inWorldScreen.hideInner());
        buf.writeInt(this.inWorldScreen.videoWidth());
        buf.writeInt(this.inWorldScreen.videoHeight());
        buf.writeBoolean(this.isRemoval);
    }

    public static void handle(ScreenLifePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                if (packet.isRemoval) {
                    ObjectSet<NarutoInWorldRenderer> renderers = ClientScreensRenderer.CLIENT_SCREENS.get(packet.inWorldScreen.dimension());
                    if (renderers != null) {
                        ObjectIterator<NarutoInWorldRenderer> renderersIterator = renderers.iterator();
                        while (renderersIterator.hasNext()) {
                            NarutoInWorldRenderer renderer = renderersIterator.next();
                            if (renderer.screen.equals(packet.inWorldScreen)) {
                                renderersIterator.remove();
                                renderer.shutdown();
                                break;
                            }
                        }
                        HiddenDisplayers.reveal(packet.inWorldScreen);
                        NarutoLoading.LOGGER.info("{}Delivered {} for removal.", NarutoLoading.info(), packet.inWorldScreen.toString());
                    }
                } else {
                    String videoPath = NarutoConfig.absolute(packet.inWorldScreen.relativeVideoPath(BaseEnv.narutoConfig.videoFileName));
                    if (!validateVideoPath(videoPath)) {
                        NarutoLoading.LOGGER.warn("{}Video file does not exist at path: {}. Skipping screen addition for {}", NarutoLoading.info(), videoPath, packet.inWorldScreen.toLocalString());
                        return;
                    }

                    Optional.ofNullable(ClientScreensRenderer.CLIENT_SCREENS.get(packet.inWorldScreen.dimension())).ifPresent(renderers -> {
                        ObjectIterator<NarutoInWorldRenderer> renderersIterator = renderers.iterator();
                        while (renderersIterator.hasNext()) {
                            NarutoInWorldRenderer renderer = renderersIterator.next();
                            if (renderer.screen.equals(packet.inWorldScreen)) {
                                renderer.shutdown();
                                renderersIterator.remove();
                                break;
                            }
                        }
                    });
                    NarutoInWorldRenderer renderer = renderer(packet);
                    ClientScreensRenderer.CLIENT_SCREENS.computeIfAbsent(packet.inWorldScreen.dimension(), key -> new ObjectOpenHashSet<>()).add(renderer);
                    if (packet.inWorldScreen.hideInner()) {
                        HiddenDisplayers.hide(packet.inWorldScreen);
                    }
                    NarutoLoading.LOGGER.info("{}Delivered {} for addition.", NarutoLoading.info(), packet.inWorldScreen.toString());
                }
            } catch (Exception exception) {
                NarutoLoading.LOGGER.error("Error handling ScreenLifePacket", exception);
            }
        });
    }

    private static boolean validateVideoPath(String videoPath) {
        if (videoPath == null || videoPath.isBlank()) {
            NarutoLoading.LOGGER.warn("{}Video path is null or blank", NarutoLoading.info());
            return false;
        }

        File videoFile = new File(videoPath);
        boolean exists = videoFile.exists() && videoFile.isFile();

        if (!exists) NarutoLoading.LOGGER.warn("{}Video file validation failed - Path: {}, Exists: {}, IsFile: {}", NarutoLoading.info(), videoPath, videoFile.exists(), videoFile.isFile());

        return exists;
    }

    private static @NotNull NarutoInWorldRenderer renderer(ScreenLifePacket packet) {
        NarutoInWorldRenderer renderer = new NarutoInWorldRenderer(packet.inWorldScreen);

        if (packet.inWorldScreen.isLocalSound()) {
            AudioConverter audioConverter = new AudioConverter(packet.inWorldScreen.relativeAudioPath(NarutoLoading.BLANK), BaseEnv.ffmpegProvider.absoluteFFmpeg, BaseEnv.ffmpegProvider.absoluteFFprobe);
            audioConverter.setup(() -> {
                ResourceZipGenerator resourceZipGenerator = new ResourceZipGenerator(audioConverter.converted);
                resourceZipGenerator.generate();
                resourceZipGenerator.reload(renderer);
            });
        } else {
            if (BaseEnv.available()) renderer.setup();
        }

        return renderer;
    }

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return TYPE;
    }
}