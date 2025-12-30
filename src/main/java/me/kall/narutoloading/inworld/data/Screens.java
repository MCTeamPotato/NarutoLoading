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
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID)
public class Screens extends SavedData {
    private static final String SCREENS_KEY = "Screens";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String CORNERS_KEY = "Corners";
    private static final String VIDEO_KEY = "Video";
    private static final String AUDIO_KEY = "Audio";
    private static final String CULLABLE_KEY = "Cullable";
    private static final String LOCAL_SOUND_KEY = "LocalSound";

    public final Object2ObjectMap<ResourceLocation, ObjectSet<InWorldScreen>> screens = new Object2ObjectOpenHashMap<>();

    public static @NotNull Screens load(@NotNull CompoundTag tag) {
        Screens screens = new Screens();

        ListTag screensList = tag.getList(SCREENS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < screensList.size(); i++) {
            CompoundTag screenTag = screensList.getCompound(i);
            ResourceLocation dimension = ResourceLocation.parse(screenTag.getString(DIMENSION_KEY));
            screens.screens.computeIfAbsent(dimension, key -> new ObjectOpenHashSet<>()).add(InWorldScreen.from(screenTag.getLongArray(CORNERS_KEY), dimension, screenTag.getString(VIDEO_KEY), screenTag.getString(AUDIO_KEY), screenTag.getBoolean(CULLABLE_KEY), screenTag.getBoolean(LOCAL_SOUND_KEY)));
        }

        return screens;
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
                screenTag.putString(VIDEO_KEY, inWorldScreen.absoluteVideoPath(""));
                screenTag.putString(AUDIO_KEY, inWorldScreen.absoluteAudioPath(""));
                screenTag.putBoolean(CULLABLE_KEY, inWorldScreen.isCullable());
                screenTag.putBoolean(LOCAL_SOUND_KEY, inWorldScreen.isLocalSound());
                NarutoLoading.LOGGER.info("Saving {} successfully", inWorldScreen.toString());
                screensList.add(screenTag);
            }
        }

        tag.put(SCREENS_KEY, screensList);
        return tag;
    }

    public static @NotNull Screens get(@NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Screens::load, Screens::new, "NarutoScreens");
    }

    @SubscribeEvent
    public static void syncScreens(PlayerEvent.@NotNull PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            PacketDistributor.PacketTarget packetTarget = PacketDistributor.PLAYER.with(() -> player);
            for (ObjectSet<InWorldScreen> inWorldScreenSet : get(level).screens.values()) {
                for (InWorldScreen inWorldScreen : inWorldScreenSet) {
                    NarutoPackets.INSTANCE.send(packetTarget, new ScreenLifePacket(inWorldScreen, false));
                }
            }
        }
    }
}