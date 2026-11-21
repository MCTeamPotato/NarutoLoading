package me.kall.narutoloading.client.fade;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;

public final class Fadable {
    private static final ObjectOpenHashSet<ResourceLocation> UNFADABLE = new ObjectOpenHashSet<>();

    public static void setUnfadable(ResourceLocation id, boolean unfadable) {
        if (unfadable) {
            UNFADABLE.add(id);
        } else {
            UNFADABLE.remove(id);
        }
    }

    public static boolean isUnfadable(ResourceLocation id) {
        return UNFADABLE.contains(id);
    }
}
