package me.kall.narutoloading.wall.client;

import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.playback.produce.audio.AudioProducer;
import me.kall.narutoloading.playback.produce.util.TimeLine;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.client.light.WallLights;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public final class WallManager {
    private static final int SCHEDULE_TICKS = 5;
    private static final int MAX_START_PER_TICK = 1;
    private static final int MAX_DESTROY_PER_TICK = 2;
    private static final long VISIBLE_MEMORY_NANOS = 4_000_000_000L;
    private static final long AUDIO_HOLD_NANOS = 1_000_000_000L;
    private static final double ACTIVE_HYSTERESIS = 1.25D;
    private static final double NANO_TO_SEC = 1_000_000_000D;

    private static final Map<ResourceLocation, Map<Wall, WallTV<?>>> DATA = new LinkedHashMap<>();

    private static final Set<Wall> TARGET = new HashSet<>();
    private static final List<Wall> PENDING = new ArrayList<>();

    private static final long CLOCK_ORIGIN = System.nanoTime();

    private static long pausedNanos;
    private static long pauseStart = -1L;

    private static int scheduleCounter;

    private WallManager() {}

    public static void add(@NotNull Wall wall) {
        Map<Wall, WallTV<?>> walls = DATA.computeIfAbsent(wall.dimension, key -> new LinkedHashMap<>());

        WallTV<?> previous = walls.put(wall, create(wall));
        if (previous != null) previous.shutdown(true);
    }

    public static boolean remove(@NotNull Wall wall) {
        Map<Wall, WallTV<?>> walls = DATA.get(wall.dimension);
        if (walls == null) return false;

        WallTV<?> removed = walls.remove(wall);
        if (walls.isEmpty()) DATA.remove(wall.dimension);
        if (removed == null) return false;

        WallLights.release(wall);
        removed.state = WallTV.State.DORMANT;
        removed.shutdown(true);
        return true;
    }

    public static void clear() {
        WallLights.releaseAll();

        for (WallTV<?> tv : list()) {
            tv.state = WallTV.State.DORMANT;
            tv.shutdown(true);
        }

        DATA.clear();
        TARGET.clear();
        PENDING.clear();
    }

    public static void clearRemote() {
        clear();
    }

    public static void applyRemote(@NotNull List<Wall> walls) {
        clear();

        for (Wall wall : walls) add(wall);
    }

    public static void putRemote(@NotNull Wall wall) {
        Map<Wall, WallTV<?>> inDimension = DATA.get(wall.dimension);
        WallTV<?> existing = inDimension == null ? null : inDimension.get(wall);

        if (existing == null) {
            add(wall);
            return;
        }

        Wall current = existing.wall;
        boolean mediaChanged = !current.media.equals(wall.media);
        current.media = wall.media;
        current.volume = wall.volume;

        if (!mediaChanged) return;

        // 换片视为全新播放，从 0 秒开始，不沿用旧片的时间轴位置。
        if (existing.isActive()) {
            existing.synchronize(true, 0D);
        } else {
            // 休眠的墙没有在播的片子，等下次激活时再从 0 秒开始。
            existing.restartOnActivate = true;
        }
    }

    public static void tick() {
        long now = System.nanoTime();
        boolean paused = Minecraft.getInstance().isPaused();

        if (paused) {
            if (pauseStart < 0L) pauseStart = now;
        } else if (pauseStart >= 0L) {
            pausedNanos += now - pauseStart;
            pauseStart = -1L;
        }

        if (++scheduleCounter >= SCHEDULE_TICKS) {
            scheduleCounter = 0;
            evaluate();
        }

        process();
        processAudio();

        for (WallTV<?> tv : list()) {
            TimeLine timeLine = tv.timeLine.get();
            if (timeLine == null) continue;

            if (paused) {
                timeLine.pause();
            } else {
                timeLine.resume();
            }
        }
    }

    public static void observe(@NotNull WallTV<?> tv, @NotNull Frustum frustum) {
        if (frustum.isVisible(tv.wall.bounds)) tv.lastVisibleNanos = System.nanoTime();
    }

    public static void rebuildActive() {
        for (WallTV<?> tv : list()) {
            if (!tv.isActive()) continue;

            TimeLine timeLine = tv.timeLine.get();
            tv.synchronize(true, timeLine == null ? 0D : timeLine.positionSec());
        }
    }

    public static double distanceTo(@NotNull Wall wall) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return Math.sqrt(wall.bounds.distanceToSqr(camera));
    }

    public static @NotNull List<WallTV<?>> getIn(@NotNull ResourceLocation dimension) {
        Map<Wall, WallTV<?>> walls = DATA.get(dimension);
        return walls == null ? List.of() : List.copyOf(walls.values());
    }

    public static @NotNull @Unmodifiable List<WallTV<?>> list() {
        List<WallTV<?>> result = new ArrayList<>();
        for (Map<Wall, WallTV<?>> walls : DATA.values()) result.addAll(walls.values());
        return List.copyOf(result);
    }

    private static void evaluate() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        TARGET.clear();
        PENDING.clear();

        if (level == null) return;

        List<WallTV<?>> walls = list();
        if (walls.isEmpty()) return;

        ResourceLocation dimension = level.dimension().location();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();

        int activeDistance = Math.max(1, NarutoConfig.worldActiveDistance());
        int maxActive = Math.max(1, NarutoConfig.worldMaxActive());
        boolean cullInvisible = NarutoConfig.worldCullInvisible();
        long now = System.nanoTime();

        List<Candidate> candidates = new ArrayList<>();

        for (WallTV<?> tv : walls) {
            if (!tv.wall.dimension.equals(dimension)) continue;

            double distance = Math.sqrt(tv.wall.bounds.distanceToSqr(camera));
            double limit = tv.isActive() ? activeDistance * ACTIVE_HYSTERESIS : activeDistance;
            if (distance > limit) continue;
            if (cullInvisible && now - tv.lastVisibleNanos > VISIBLE_MEMORY_NANOS) continue;

            candidates.add(new Candidate(tv, distance));
        }

        candidates.sort(Comparator.comparingDouble(candidate -> candidate.distance));

        for (int index = 0; index < candidates.size() && index < maxActive; index++) {
            WallTV<?> tv = candidates.get(index).tv;

            TARGET.add(tv.wall);
            if (!tv.isActive()) PENDING.add(tv.wall);
        }

        boolean audioEnabled = NarutoConfig.worldAudioEnabled();
        int audioDistance = Math.max(1, NarutoConfig.worldAudioMaxDistance());
        int maxAudio = Math.max(1, NarutoConfig.worldMaxAudio());

        Set<Wall> audible = new HashSet<>();

        if (audioEnabled) {
            for (Candidate candidate : candidates) {
                if (audible.size() >= maxAudio) break;
                if (!candidate.tv.isActive() || candidate.distance > audioDistance) continue;
                if (now - candidate.tv.audioChangedNanos >= AUDIO_HOLD_NANOS) continue;
                if (candidate.tv.audioSelected) audible.add(candidate.tv.wall);
            }

            for (Candidate candidate : candidates) {
                if (audible.size() >= maxAudio) break;
                if (!candidate.tv.isActive() || candidate.distance > audioDistance) continue;
                if (audible.contains(candidate.tv.wall)) continue;
                if (now - candidate.tv.audioChangedNanos < AUDIO_HOLD_NANOS) continue;

                audible.add(candidate.tv.wall);
            }
        }

        for (WallTV<?> tv : walls) {
            if (!tv.isActive() || !tv.wall.dimension.equals(dimension)) continue;

            boolean desired = audible.contains(tv.wall);
            if (tv.audioSelected == desired) continue;

            tv.audioSelected = desired;
            tv.audioChangedNanos = now;
        }
    }

    private static void processAudio() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        ResourceLocation dimension = level.dimension().location();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();

        for (WallTV<?> tv : list()) {
            if (!tv.isActive() || !tv.wall.dimension.equals(dimension)) continue;

            tv.setAudioEnabled(tv.audioSelected);

            AudioProducer audio = tv.audio.get();
            if (audio == null) continue;

            audio.volume(gain(tv.wall, Math.sqrt(tv.wall.bounds.distanceToSqr(camera))));
        }
    }

    private static float gain(@NotNull Wall wall, double distance) {
        float falloff = falloff(distance);
        if (falloff <= 0F) return 0F;

        float master = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
        return NarutoConfig.worldVolume() * wall.volume * master * falloff;
    }

    private static float falloff(double distance) {
        double max = Math.max(1D, NarutoConfig.worldAudioMaxDistance());
        double reference = max * 0.15D;

        if (distance <= reference) return 1F;
        if (distance >= max) return 0F;

        double remaining = 1D - (distance - reference) / (max - reference);
        return (float) (remaining * remaining);
    }

    private static void process() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;

        ResourceLocation dimension = level.dimension().location();

        int destroyed = 0;
        for (WallTV<?> tv : list()) {
            if (!tv.isActive() || TARGET.contains(tv.wall)) continue;

            deactivate(tv);
            if (++destroyed >= MAX_DESTROY_PER_TICK) break;
        }

        int started = 0;
        while (started < MAX_START_PER_TICK && !PENDING.isEmpty()) {
            Wall wall = PENDING.remove(0);
            if (!wall.dimension.equals(dimension)) continue;

            WallTV<?> tv = find(wall);
            if (tv == null || tv.isActive() || !TARGET.contains(wall)) continue;

            activate(tv);
            started++;
        }
    }

    private static void activate(@NotNull WallTV<?> tv) {
        boolean restart = tv.restartOnActivate;

        tv.restartOnActivate = false;
        tv.audioSelected = false;
        tv.audioChangedNanos = 0L;
        tv.state = WallTV.State.ACTIVE;
        tv.setup(restart ? 0D : seekFor(tv.wall));
    }

    private static void deactivate(@NotNull WallTV<?> tv) {
        WallLights.release(tv.wall);
        tv.audioSelected = false;
        tv.audioChangedNanos = 0L;
        tv.state = WallTV.State.DORMANT;
        tv.shutdown(true);
    }

    private static @Nullable WallTV<?> find(@NotNull Wall wall) {
        Map<Wall, WallTV<?>> walls = DATA.get(wall.dimension);
        return walls == null ? null : walls.get(wall);
    }

    public static @Nullable WallTV<?> get(@NotNull Wall wall) {
        return find(wall);
    }

    private static double seekFor(@NotNull Wall wall) {
        MediaRepository media = NarutoContext.get().media();
        MediaRepository.Media current = wall.media.isBlank() ? media.current() : media.find(wall.media);
        double duration = current == null || current.video() == null ? 0D : current.video().duration();

        if (!(duration > 0D)) return 0D;
        return clockSeconds() % duration;
    }

    private static double clockSeconds() {
        long now = System.nanoTime();
        long paused = pausedNanos + (pauseStart < 0L ? 0L : now - pauseStart);
        return (double) (now - CLOCK_ORIGIN - paused) / NANO_TO_SEC;
    }

    private static @NotNull WallTV<?> create(@NotNull Wall wall) {
        return new WallTV.Buffer(wall, NarutoContext.get().media());
    }

    private record Candidate(WallTV<?> tv, double distance) {}
}
