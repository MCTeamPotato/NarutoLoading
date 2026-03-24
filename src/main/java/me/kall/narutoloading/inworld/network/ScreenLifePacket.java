package me.kall.narutoloading.inworld.network;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.ffmpeg.AudioConverter;
import me.kall.narutoloading.common.util.Paths;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.inworld.data.HiddenDisplayers;
import me.kall.narutoloading.inworld.gui.util.ResourceZipGenerator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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
        this.inWorldScreen = InWorldScreen.from(buf.readLongArray(), buf.readResourceLocation(), buf.readUtf(), buf.readUtf(), buf.readResourceLocation(), buf.readFloat(), buf.readBoolean(), buf.readInt(), buf.readInt());
        this.isRemoval = buf.readBoolean();
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeLongArray(this.inWorldScreen.toLongArray());
        buf.writeResourceLocation(this.inWorldScreen.dimension());
        buf.writeUtf(this.inWorldScreen.relativeVideoPath(NarutoLoading.BLANK));
        buf.writeUtf(this.inWorldScreen.relativeAudioPath(NarutoLoading.BLANK));
        buf.writeResourceLocation(this.inWorldScreen.localSound());
        buf.writeFloat(this.inWorldScreen.soundVolume());
        buf.writeBoolean(this.inWorldScreen.hideInner());
        buf.writeInt(this.inWorldScreen.videoWidth());
        buf.writeInt(this.inWorldScreen.videoHeight());
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
                        HiddenDisplayers.reveal(this.inWorldScreen);
                    }
                } else {
                    String videoPath = Paths.absolute(this.inWorldScreen.relativeVideoPath(BaseEnv.narutoConfig.videoFileName));
                    if (!validateVideoPath(videoPath)) {
                        return;
                    }

                    Optional.ofNullable(ClientScreensRenderer.CLIENT_SCREENS.get(this.inWorldScreen.dimension())).ifPresent(renderers -> {
                        ObjectIterator<NarutoInWorldRenderer> renderersIterator = renderers.iterator();
                        while (renderersIterator.hasNext()) {
                            NarutoInWorldRenderer renderer = renderersIterator.next();
                            if (renderer.screen.equals(this.inWorldScreen)) {
                                renderer.shutdown();
                                renderersIterator.remove();
                                break;
                            }
                        }
                    });
                    NarutoInWorldRenderer renderer = renderer();
                    ClientScreensRenderer.CLIENT_SCREENS.computeIfAbsent(this.inWorldScreen.dimension(), key -> new ObjectOpenHashSet<>()).add(renderer);
                    if (this.inWorldScreen.hideInner()) {
                        HiddenDisplayers.hide(this.inWorldScreen);
                    }
                }
            } catch (Exception ignored) {}
        });
        ctx.get().setPacketHandled(true);
    }

    private boolean validateVideoPath(String videoPath) {
        if (videoPath == null || videoPath.isBlank()) {
            return false;
        }

        File videoFile = new File(videoPath);
        boolean exists = videoFile.exists() && videoFile.isFile();
        return exists;
    }

    private @NotNull NarutoInWorldRenderer renderer() {
        NarutoInWorldRenderer renderer = new NarutoInWorldRenderer(this.inWorldScreen);

        if (this.inWorldScreen.isLocalSound()) {
            AudioConverter audioConverter = new AudioConverter(this.inWorldScreen.relativeAudioPath(NarutoLoading.BLANK), BaseEnv.ffmpegProvider.absoluteFFmpeg, BaseEnv.ffmpegProvider.absoluteFFprobe);
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
}