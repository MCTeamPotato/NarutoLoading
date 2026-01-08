package me.kall.narutoloading.inworld.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import me.kall.narutoloading.inworld.network.ScreenLifePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID)
public class Screens extends SavedData {
    private static final String SCREENS_KEY = "Screens";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String CORNERS_KEY = "Corners";
    private static final String VIDEO_KEY = "Video";
    private static final String AUDIO_KEY = "Audio";
    private static final String LOCAL_SOUND_KEY = "LocalSound";
    private static final String SOUND_VOLUME = "SoundVolume";
    private static final String HIDE_INNER_KEY = "HideInner";
    private static final String VIDEO_WIDTH_KEY = "VideoWidth";
    private static final String VIDEO_HEIGHT_KEY = "VideoHeight";

    public final Object2ObjectMap<ResourceLocation, ObjectSet<InWorldScreen>> screens = new Object2ObjectOpenHashMap<>();

    public Screens() {
        super("NarutoScreens");
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        Screens screens = new Screens();

        ListTag screensList = tag.getList(SCREENS_KEY, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < screensList.size(); i++) {
            CompoundTag screenTag = screensList.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(screenTag.getString(DIMENSION_KEY));
            screens.screens.computeIfAbsent(dimension, key -> new ObjectOpenHashSet<>()).add(InWorldScreen.from(screenTag.getLongArray(CORNERS_KEY), dimension, screenTag.getString(VIDEO_KEY), screenTag.getString(AUDIO_KEY), ResourceLocation.tryParse(screenTag.getString(LOCAL_SOUND_KEY)), screenTag.getFloat(SOUND_VOLUME), screenTag.getBoolean(HIDE_INNER_KEY), screenTag.getInt(VIDEO_WIDTH_KEY), screenTag.getInt(VIDEO_HEIGHT_KEY)));
        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag screensList = new ListTag();

        for (Object2ObjectMap.Entry<ResourceLocation, ObjectSet<InWorldScreen>> entry : this.screens.object2ObjectEntrySet()) {
            ResourceLocation dimension = entry.getKey();

            for (InWorldScreen inWorldScreen : entry.getValue()) {
                CompoundTag screenTag = new CompoundTag();
                screenTag.putString(DIMENSION_KEY, dimension.toString());
                screenTag.putLongArray(CORNERS_KEY, inWorldScreen.toLongArray());
                screenTag.putString(VIDEO_KEY, inWorldScreen.relativeVideoPath(NarutoLoading.BLANK));
                screenTag.putString(AUDIO_KEY, inWorldScreen.relativeAudioPath(NarutoLoading.BLANK));
                screenTag.putString(LOCAL_SOUND_KEY, Optional.ofNullable(inWorldScreen.localSound()).orElse(InWorldScreen.NO_LOCAL_SOUND).toString());
                screenTag.putFloat(SOUND_VOLUME, inWorldScreen.soundVolume());
                screenTag.putBoolean(HIDE_INNER_KEY, inWorldScreen.hideInner());
                screenTag.putInt(VIDEO_HEIGHT_KEY, inWorldScreen.videoHeight());
                screenTag.putInt(VIDEO_WIDTH_KEY, inWorldScreen.videoWidth());
                NarutoLoading.LOGGER.info("{}Saving {} successfully", NarutoLoading.info(), inWorldScreen.toString());
                screensList.add(screenTag);
            }
        }

        tag.put(SCREENS_KEY, screensList);
        return tag;
    }

    public static @NotNull Screens get(@NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Screens::new, "NarutoScreens");
    }

    @SubscribeEvent
    public static void syncScreens(PlayerEvent.@NotNull PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer && event.getPlayer().level instanceof ServerLevel) {
            ServerLevel level = (ServerLevel) event.getPlayer().level;
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            PacketDistributor.PacketTarget packetTarget = PacketDistributor.PLAYER.with(() -> player);
            for (ObjectSet<InWorldScreen> inWorldScreenSet : get(level).screens.values()) {
                for (InWorldScreen inWorldScreen : inWorldScreenSet) {
                    NarutoPackets.INSTANCE.send(packetTarget, new ScreenLifePacket(inWorldScreen, false));
                }
            }
        }
    }
}