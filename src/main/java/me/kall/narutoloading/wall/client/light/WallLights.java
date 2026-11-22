package me.kall.narutoloading.wall.client.light;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.wall.Wall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WallLights {
    public static final int MAX_DISTANCE = 32;

    private static final int MAX_CHECKS_PER_FRAME = 128;
    private static final long SAMPLE_INTERVAL_NANOS = 100_000_000L;
    private static final float EMA = 0.35F;
    private static final float HYSTERESIS = 1.0F;
    private static final int SAMPLES = 8;

    private static final Long2ByteOpenHashMap LIVE = new Long2ByteOpenHashMap();
    private static volatile Long2ByteMap snapshot = new Long2ByteOpenHashMap();
    private static volatile boolean any;

    private static final Object2ObjectOpenHashMap<Wall, State> STATES = new Object2ObjectOpenHashMap<>();
    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();

    private WallLights() {}

    public static int emissionAt(@Nullable BlockGetter level, @Nullable BlockPos pos) {
        if (!any || pos == null || !(level instanceof ClientLevel)) return 0;
        return snapshot.get(pos.asLong()) & 0xFF;
    }

    public static void sample(@NotNull Wall wall, int width, int height, @Nullable ByteBuffer y) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || !WallLightHook.active() || !NarutoConfig.worldLightEnabled() || !wall.light || !wall.dimension.equals(level.dimension().location())) {
            release(wall);
            return;
        }

        double distance = wall.bounds.distanceToSqr(minecraft.gameRenderer.getMainCamera().getPosition());
        if (distance > (double) MAX_DISTANCE * MAX_DISTANCE) {
            release(wall);
            return;
        }

        long now = System.nanoTime();
        State state = STATES.get(wall);

        if (state != null && now - state.sampledNanos < SAMPLE_INTERVAL_NANOS) return;
        if (state == null) {
            state = create(wall);
            STATES.put(wall, state);
        }

        state.sampledNanos = now;
        sampleCells(state, width, height, y);
    }

    public static void release(@NotNull Wall wall) {
        State state = STATES.remove(wall);
        if (state == null) return;

        List<Long> changed = null;

        for (long position : state.positions) {
            if (LIVE.remove(position) == 0) continue;
            if (changed == null) changed = new ArrayList<>();
            changed.add(position);
        }

        publish();
        if (changed != null) enqueue(changed);
    }

    public static void releaseAll() {
        if (STATES.isEmpty()) return;

        for (Wall wall : new ArrayList<>(STATES.keySet())) release(wall);
    }

    public static void tick() {
        if (!WallLightHook.active() || !NarutoConfig.worldLightEnabled()) {
            releaseAll();
            return;
        }

        if (PENDING.isEmpty()) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            PENDING.clear();
            releaseAll();
            return;
        }

        int budget = MAX_CHECKS_PER_FRAME;

        while (budget-- > 0 && !PENDING.isEmpty()) {
            level.getLightEngine().checkBlock(BlockPos.of(PENDING.poll()));
        }
    }

    private static @NotNull State create(@NotNull Wall wall) {
        int cols = wall.faceWidth();
        int rows = wall.faceHeight();

        long[] positions = new long[cols * rows];

        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                positions[index++] = switch (wall.axisThickness) {
                    case X -> BlockPos.asLong(wall.minX, wall.minY + row, wall.minZ + col);
                    case Y -> BlockPos.asLong(wall.minX + col, wall.minY, wall.minZ + row);
                    default -> BlockPos.asLong(wall.minX + col, wall.minY + row, wall.minZ);
                };
            }
        }

        return new State(positions, new float[positions.length], new byte[positions.length], cols, rows);
    }

    private static void sampleCells(@NotNull State state, int width, int height, @Nullable ByteBuffer y) {
        int stepX = Math.max(1, width / state.cols / SAMPLES);
        int stepY = Math.max(1, height / state.rows / SAMPLES);

        List<Long> changed = null;

        for (int row = 0; row < state.rows; row++) {
            int baseY = Math.min(height - 1, row * height / state.rows);

            for (int col = 0; col < state.cols; col++) {
                int baseX = Math.min(width - 1, col * width / state.cols);

                float total = 0F;

                for (int sy = 0; sy < SAMPLES; sy++) {
                    int py = Math.min(height - 1, baseY + sy * stepY);

                    for (int sx = 0; sx < SAMPLES; sx++) {
                        int px = Math.min(width - 1, baseX + sx * stepX);
                        total += luminance(px, py, width, y);
                    }
                }

                int index = row * state.cols + col;
                float target = total / (SAMPLES * SAMPLES) / 255F * 15F;
                float ema = state.ema[index] + (target - state.ema[index]) * EMA;
                state.ema[index] = ema;

                int level = state.levels[index];
                if (Math.abs(ema - level) < HYSTERESIS) continue;

                int next = Math.max(0, Math.min(15, Math.round(ema)));
                if (next == level) continue;

                state.levels[index] = (byte) next;
                long position = state.positions[index];

                if (next > 0) LIVE.put(position, (byte) next);
                else LIVE.remove(position);

                if (changed == null) changed = new ArrayList<>();
                changed.add(position);
            }
        }

        if (changed == null) return;

        publish();
        enqueue(changed);
    }

    private static float luminance(int x, int y, int width, @Nullable ByteBuffer planes) {
        if (planes == null) return 0F;
        return planes.get(y * width + x) & 0xFF;
    }

    private static void publish() {
        snapshot = new Long2ByteOpenHashMap(LIVE);
        any = !LIVE.isEmpty();
    }

    private static void enqueue(@NotNull List<Long> positions) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        positions.sort(Comparator.comparingDouble(position -> BlockPos.of(position).distToCenterSqr(camera.x, camera.y, camera.z)));
        PENDING.addAll(positions);
    }

    private static final class State {
        private final long[] positions;
        private final float[] ema;
        private final byte[] levels;
        private final int cols, rows;
        private long sampledNanos;

        private State(long[] positions, float[] ema, byte[] levels, int cols, int rows) {
            this.positions = positions;
            this.ema = ema;
            this.levels = levels;
            this.cols = cols;
            this.rows = rows;
        }
    }
}
