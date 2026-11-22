package me.kall.narutoloading.server;

import me.kall.narutoloading.config.api.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class ServerConfig {
    public static final String WALL_BLOCKS = "wallBlocks";
    public static final String BUILDER_ITEM = "builderItem";
    public static final String WALL_MAX_SIDE = "wallMaxSide";
    public static final String WALL_MAX_AREA = "wallMaxArea";
    public static final String EDIT_DISTANCE = "editDistance";

    private static final String DEFAULT_WALL_BLOCKS = "minecraft:stone,minecraft:stone_bricks,minecraft:chiseled_stone_bricks,minecraft:smooth_stone,minecraft:quartz_block,minecraft:white_concrete,minecraft:iron_block,minecraft:glass";

    private static final Config CONFIG = create();
    private static final Set<String> WALL_BLOCK_SET = parseBlocks(CONFIG.getString(WALL_BLOCKS, DEFAULT_WALL_BLOCKS));

    private ServerConfig() {}

    private static @NotNull Config create() {
        try {
            return defaults(Config.builder().file("config/narutoloading-server.properties")).autoCreate(true).build();
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading] Exception loading the server config file, falling back to in-memory defaults: " + throwable.getMessage());
            return defaults(Config.builder()).build();
        }
    }

    private static @NotNull Config.Builder defaults(@NotNull Config.Builder builder) {
        return builder.prefix("narutoloading.server.")
                .header("NarutoLoading server settings.\nwallBlocks: comma separated block ids that may form a wall frame.\nbuilderItem: item id used to select wall corners while sneaking.")
                .defaultValue(WALL_BLOCKS, DEFAULT_WALL_BLOCKS)
                .defaultValue(BUILDER_ITEM, "minecraft:stick")
                .defaultValue(WALL_MAX_SIDE, 64)
                .defaultValue(WALL_MAX_AREA, 2048)
                .defaultValue(EDIT_DISTANCE, 64);
    }

    public static @NotNull Set<String> wallBlocks() {
        return WALL_BLOCK_SET;
    }

    public static @NotNull String builderItem() {
        return CONFIG.getString(BUILDER_ITEM, "minecraft:stick").trim().toLowerCase(Locale.ROOT);
    }

    public static int wallMaxSide() {
        return Math.max(1, CONFIG.getInt(WALL_MAX_SIDE, 64));
    }

    public static int wallMaxArea() {
        return Math.max(1, CONFIG.getInt(WALL_MAX_AREA, 2048));
    }

    public static int editDistance() {
        return Math.max(1, CONFIG.getInt(EDIT_DISTANCE, 64));
    }

    private static @NotNull @UnmodifiableView Set<String> parseBlocks(@NotNull String value) {
        Set<String> blocks = new LinkedHashSet<>();

        for (String part : value.split(",")) {
            String id = part.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) blocks.add(id);
        }

        return Collections.unmodifiableSet(blocks);
    }
}
