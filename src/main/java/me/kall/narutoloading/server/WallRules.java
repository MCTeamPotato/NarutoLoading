package me.kall.narutoloading.server;

import me.kall.narutoloading.world.Wall;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public final class WallRules {
    private static final int MEDIA_LIMIT = 256;

    private WallRules() {}

    public static boolean canEdit(@NotNull ServerPlayer player, @NotNull Wall wall) {
        double limit = ServerConfig.editDistance();
        return wall.bounds.distanceToSqr(player.position()) <= limit * limit;
    }

    public static boolean validMedia(@NotNull String media) {
        if (media.length() > MEDIA_LIMIT) return false;

        for (int index = 0; index < media.length(); index++) {
            char character = media.charAt(index);
            if (character < 0x20 || character == '/' || character == '\\') return false;
        }

        return true;
    }

    public static boolean isFrame(@NotNull BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && ServerConfig.wallBlocks().contains(id.toString());
    }

    public static boolean isBuilderItem(@NotNull ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.toString().equals(ServerConfig.builderItem());
    }
}
