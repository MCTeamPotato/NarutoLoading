package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.inworld.gui.InWorldSelectionScreen;
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
        this.inWorldScreen = InWorldScreen.from(buf.readLongArray(), buf.readResourceLocation(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readResourceLocation());
        this.isRemoval = buf.readBoolean();
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLongArray(this.inWorldScreen.toLongArray());
        buf.writeResourceLocation(this.inWorldScreen.dimension());
        buf.writeUtf(this.inWorldScreen.absoluteVideoPath(""));
        buf.writeUtf(this.inWorldScreen.absoluteAudioPath(""));
        buf.writeBoolean(this.inWorldScreen.isCullable());
        buf.writeResourceLocation(this.inWorldScreen.getLocalSound());
        buf.writeBoolean(this.isRemoval);
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                if (this.isRemoval) {
                    ObjectSet<NarutoInWorldRenderer> renderers = ClientScreensRenderer.CLIENT_SCREENS.get(this.inWorldScreen.dimension());
                    if (renderers != null) {
                        ObjectIterator<NarutoInWorldRenderer> renderersIterator = renderers.iterator();
                        while (renderersIterator.hasNext()) {
                            NarutoInWorldRenderer renderer = renderersIterator.next();
                            if (renderer.screen.equals(this.inWorldScreen)) {
                                renderersIterator.remove();
                                renderer.shutdown();
                                break;
                            }
                        }
                        NarutoLoading.LOGGER.info("{}Delivered {} for removal.", NarutoLoading.info(), this.inWorldScreen.toString());
                    }
                } else {
                    Optional.ofNullable(ClientScreensRenderer.CLIENT_SCREENS.get(this.inWorldScreen.dimension())).ifPresent(renderers -> {
                        ObjectIterator<NarutoInWorldRenderer> renderersIterator = renderers.iterator();
                        while (renderersIterator.hasNext()) {
                            NarutoInWorldRenderer renderer = renderersIterator.next();
                            if (renderer.screen.equals(this.inWorldScreen)) {
                                renderersIterator.remove();
                                renderer.shutdown();
                                break;
                            }
                        }
                    });
                    ClientScreensRenderer.CLIENT_SCREENS.computeIfAbsent(this.inWorldScreen.dimension(), key -> new ObjectOpenHashSet<>()).add(renderer());
                    NarutoLoading.LOGGER.info("{}Delivered {} for addition.", NarutoLoading.info(), this.inWorldScreen.toString());
                }
            } catch (Exception exception) {
                NarutoLoading.LOGGER.error("Error handling ScreenLifePacket", exception);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private @NotNull NarutoInWorldRenderer renderer() {
        NarutoInWorldRenderer renderer = new NarutoInWorldRenderer(this.inWorldScreen);

        if (this.inWorldScreen.isLocalSound()) {
            InWorldSelectionScreen.AudioConverter audioConverter = new InWorldSelectionScreen.AudioConverter(this.inWorldScreen.absoluteAudioPath(""), BaseEnv.ffmpegProvider.absoluteFFmpeg);
            audioConverter.setup(() -> {
                InWorldSelectionScreen.ResourceZipGenerator resourceZipGenerator = new InWorldSelectionScreen.ResourceZipGenerator(audioConverter.converted);
                resourceZipGenerator.generate();
                resourceZipGenerator.reload(renderer);
            });
        } else {
            if (BaseEnv.available()) renderer.setup();
        }

        return renderer;
    }
}
