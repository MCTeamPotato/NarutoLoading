package me.kall.narutoloading.wall.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.kall.narutoloading.network.message.WallRemoveMessage;
import me.kall.narutoloading.network.message.WallSyncMessage;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.data.WallStorage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class WallCommands {
    private WallCommands() {}

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("narutoloading")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("wall")
                        .then(Commands.literal("list").executes(WallCommands::list))
                        .then(Commands.literal("info")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(WallCommands::info)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(WallCommands::remove)))
                        .then(Commands.literal("clear").executes(WallCommands::clear))
                        .then(Commands.literal("add")
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                .executes(context -> add(context, ""))
                                                .then(Commands.argument("media", StringArgumentType.greedyString())
                                                        .executes(context -> add(context, StringArgumentType.getString(context, "media")))))))));
    }

    private static int add(@NotNull CommandContext<CommandSourceStack> context, @NotNull String media) throws CommandSyntaxException {
        BlockPos from = BlockPosArgument.getLoadedBlockPos(context, "from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(context, "to");

        ServerLevel level = context.getSource().getLevel();
        Wall wall = WallBuilder.build(level, from, to, media.trim());

        if (wall == null) {
            context.getSource().sendFailure(Component.translatable("narutoloading.message.wall.invalid"));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("narutoloading.message.wall.built", from.toShortString(), to.toShortString()), true);
        return 1;
    }

    private static int list(@NotNull CommandContext<CommandSourceStack> context) {
        List<Wall> walls = WallStorage.get(context.getSource().getLevel()).all();

        if (walls.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("narutoloading.message.wall.list.empty"), false);
            return 0;
        }

        int count = walls.size();
        context.getSource().sendSuccess(() -> Component.translatable("narutoloading.message.wall.list", count), false);

        for (int index = 0; index < count; index++) {
            Wall wall = walls.get(index);
            String text = "#" + (index + 1) + " " + wall.leftBottom.toShortString() + " -> " + wall.rightBottom.toShortString()
                    + ", media='" + (wall.media.isBlank() ? "<current>" : wall.media) + "'"
                    + ", volume=" + String.format(Locale.ROOT, "%.2f", wall.volume)
                    + ", size=" + wall.widthX + "x" + wall.widthY + "x" + wall.widthZ;

            context.getSource().sendSuccess(() -> Component.literal(text), false);
        }

        return count;
    }

    private static int info(@NotNull CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Wall wall = find(context);
        if (wall == null) {
            context.getSource().sendFailure(Component.translatable("narutoloading.message.wall.missing"));
            return 0;
        }

        String text = wall.leftBottom.toShortString() + " -> " + wall.rightBottom.toShortString()
                + ", media='" + (wall.media.isBlank() ? "<current>" : wall.media) + "'"
                + ", volume=" + String.format(Locale.ROOT, "%.2f", wall.volume)
                + ", size=" + wall.widthX + "x" + wall.widthY + "x" + wall.widthZ
                + ", area=" + wall.areaInvolved().size();

        context.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int remove(@NotNull CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Wall wall = find(context);
        if (wall == null) {
            context.getSource().sendFailure(Component.translatable("narutoloading.message.wall.missing"));
            return 0;
        }

        ServerLevel level = context.getSource().getLevel();
        WallStorage.get(level).remove(wall);

        PacketDistributor.sendToPlayersInDimension(level, new WallRemoveMessage(wall.toLongArray()));
        context.getSource().sendSuccess(() -> Component.translatable("narutoloading.message.wall.removed"), true);
        return 1;
    }

    private static int clear(@NotNull CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        WallStorage storage = WallStorage.get(level);

        List<Wall> walls = storage.all();
        for (Wall wall : walls) storage.remove(wall);

        PacketDistributor.sendToPlayersInDimension(level, new WallSyncMessage(new ListTag()));

        int count = walls.size();
        context.getSource().sendSuccess(() -> Component.translatable("narutoloading.message.wall.cleared", count), true);
        return count;
    }

    private static @Nullable Wall find(@NotNull CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        return WallStorage.get(context.getSource().getLevel()).get(pos.asLong());
    }
}
