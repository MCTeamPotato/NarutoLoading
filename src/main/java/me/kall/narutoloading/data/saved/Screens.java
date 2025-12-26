package me.kall.narutoloading.data.saved;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.core.detection.inworld.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class Screens extends SavedData {
    public final Object2ObjectMap<ResourceLocation, ObjectSet<Screen>> screens = new Object2ObjectOpenHashMap<>();

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        return compoundTag;
    }
}
