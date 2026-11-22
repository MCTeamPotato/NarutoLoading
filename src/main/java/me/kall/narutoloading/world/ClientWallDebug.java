package me.kall.narutoloading.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.kall.narutoloading.playback.produce.util.TimeLine;
import me.kall.narutoloading.playback.produce.video.AbstractFrameProducer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public final class ClientWallDebug {
    private ClientWallDebug() {}

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("narutoloading")
                .then(Commands.literal("wall")
                        .then(Commands.literal("debug").executes(ClientWallDebug::debug))));
    }

    private static int debug(CommandContext<CommandSourceStack> context) {
        List<WallTV<?>> walls = WallManager.list();
        if (walls.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No walls are registered."), false);
            return 0;
        }

        int active = 0;
        for (WallTV<?> tv : walls) if (tv.isActive()) active++;

        String summary = "Walls: " + walls.size() + ", active: " + active + ", dormant: " + (walls.size() - active);
        context.getSource().sendSuccess(() -> Component.literal(summary), false);

        for (WallTV<?> tv : walls) {
            if (!tv.isActive()) continue;

            Wall wall = tv.wall;
            TimeLine timeLine = tv.timeLine.get();
            AbstractFrameProducer<?> video = tv.video.get();

            double position = timeLine == null ? 0D : timeLine.positionSec();
            int buffered = video == null ? 0 : video.bufferedFrames();
            String media = wall.media.isBlank() ? "<current>" : wall.media;
            String mode = tv instanceof WallTV.Buffer ? "buffer" : "image";
            String text = "- " + media + ", " + String.format(Locale.ROOT, "%.1f", WallManager.distanceTo(wall)) + "m, " + mode + ", " + String.format(Locale.ROOT, "%.1f", position) + "s, buffered=" + buffered;

            context.getSource().sendSuccess(() -> Component.literal(text), false);
        }

        return active;
    }
}
