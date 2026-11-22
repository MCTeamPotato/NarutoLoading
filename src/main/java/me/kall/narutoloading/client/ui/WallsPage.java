package me.kall.narutoloading.client.ui;

import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.view.View;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.network.NarutoNetwork;
import me.kall.narutoloading.network.message.WallCleanRequestMessage;
import me.kall.narutoloading.network.message.WallRemoveRequestMessage;
import me.kall.narutoloading.network.message.WallUpdateMessage;
import me.kall.narutoloading.config.NarutoSettings;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.client.WallManager;
import me.kall.narutoloading.wall.client.WallTV;
import me.kall.narutoloading.wall.client.light.WallLightHook;
import me.kall.narutoloading.wall.geometry.WallGeometry;
import me.kall.narutoui.mui.choice.MuiOption;
import me.kall.narutoui.mui.screen.MuiDialog;
import me.kall.narutoui.mui.screen.MuiScreenHost;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * In-world video walls (WallTV): one wall selected from the crosshair or the list, plus the global
 * rendering, performance and audio defaults. Loading-screen settings are deliberately absent.
 */
final class WallsPage implements SettingsPageBuilder {
    private final NarutoScreen screen;
    private final NarutoContext context;

    private @Nullable Wall selected;
    private String pendingMedia = "";
    private float pendingVolume = 1.0F;
    private boolean pendingLight;
    private boolean dirty;

    private @Nullable View anchor;
    private int sendGeneration;

    WallsPage(@NotNull NarutoScreen screen, @NotNull NarutoContext context) {
        this.screen = screen;
        this.context = context;
    }

    @Override
    public void build(@NotNull MuiSettingsPage page) {
        page.shimmerAll();

        this.anchor = page.context().content();

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || minecraft.getConnection() == null || !NarutoNetwork.CHANNEL.isRemotePresent(minecraft.getConnection().getConnection())) {
            page.section("narutoloading.walls.section.selected", section -> section
                    .note("narutoloading.walls.unavailable", "narutoloading.walls.unavailable.desc")
                    .note("narutoloading.walls.build", "narutoloading.walls.build.desc"));
        } else {
            List<Wall> walls = walls(level);
            this.resolveSelection(walls);
            Wall wall = this.selected;

            page.section("narutoloading.walls.section.selected", section -> {
                if (walls.isEmpty()) {
                    section.note("narutoloading.walls.empty", "narutoloading.walls.empty.desc");
                    return;
                }

                List<MuiOption<Wall>> options = new ArrayList<>(walls.size());
                for (int index = 0; index < walls.size(); index++) options.add(MuiOption.of(walls.get(index), label(walls.get(index), index)));

                section.dropdown("narutoloading.walls.target", "narutoloading.walls.target.desc", options, wall, this::selectWall);

                if (wall != null) section.description("narutoloading.walls.position", describe(wall));
            });

            if (wall != null) {
                page.section("narutoloading.walls.section.wall", section -> {
                    List<MuiOption<String>> options = new ArrayList<>();
                    options.add(MuiOption.of("", I18n.get("narutoloading.walls.media.follow")));

                    for (MediaRepository.Media media : this.context.media().all()) options.add(MuiOption.of(media.name(), media.name()));

                    section.dropdown("narutoloading.walls.media", "narutoloading.walls.media.desc", options, this.pendingMedia, value -> {
                                this.pendingMedia = value;
                                this.markDirty();
                            })
                            .slider("narutoloading.walls.volume", "narutoloading.walls.volume.desc", 0, 200, () -> Math.round(this.pendingVolume * 100.0F), value -> {
                                this.pendingVolume = value / 100.0F;
                                this.markDirty();
                            }, value -> value + "%")
                            .toggle("narutoloading.walls.light", "narutoloading.walls.light.desc", () -> this.pendingLight, value -> {
                                this.pendingLight = value;
                                this.markDirty();
                            })
                            .enabledWhen(WallLightHook::active);

                    if (!WallLightHook.active()) section.note("narutoloading.walls.light.unavailable", "narutoloading.walls.light.unavailable.desc");
                });

                page.section("narutoloading.walls.section.actions", section -> section
                        .button("narutoloading.walls.restart", true, this::restart)
                        .button("narutoloading.walls.clean", false, this::clean)
                        .button("narutoloading.walls.remove", false, this::remove));
            }
        }

        page.collapsibleCard("narutoloading.walls.render", "narutoloading.walls.section.render", "narutoloading.walls.render.summary", true, null, section -> section
                        .number("narutoloading.walls.width", "narutoloading.walls.width.desc", 16, 3840, () -> this.screen.settings().get(NarutoSettings.WORLD_MAX_WIDTH), value -> this.screen.set(NarutoSettings.WORLD_MAX_WIDTH, value))
                        .number("narutoloading.walls.height", "narutoloading.walls.height.desc", 16, 2160, () -> this.screen.settings().get(NarutoSettings.WORLD_MAX_HEIGHT), value -> this.screen.set(NarutoSettings.WORLD_MAX_HEIGHT, value))
                        .decimal("narutoloading.walls.buffer", "narutoloading.walls.buffer.desc", 0.0D, 5.0D, () -> this.screen.settings().get(NarutoSettings.WORLD_BUFFER_SECONDS), value -> this.screen.set(NarutoSettings.WORLD_BUFFER_SECONDS, value))
                        .toggle("narutoloading.walls.cullFaces", "narutoloading.walls.cullFaces.desc", () -> this.screen.settings().get(NarutoSettings.WORLD_CULL_FACES), value -> this.screen.set(NarutoSettings.WORLD_CULL_FACES, value))
                        .toggle("narutoloading.walls.lightEnabled", "narutoloading.walls.lightEnabled.desc", () -> this.screen.settings().get(NarutoSettings.WORLD_LIGHT_ENABLED), value -> this.screen.set(NarutoSettings.WORLD_LIGHT_ENABLED, value))
                        .enabledWhen(WallLightHook::active));

        page.collapsibleCard("narutoloading.walls.performance", "narutoloading.walls.section.performance", "narutoloading.walls.performance.summary", false, null, section -> section
                        .number("narutoloading.walls.distance", "narutoloading.walls.distance.desc", 4, 256, () -> this.screen.settings().get(NarutoSettings.WORLD_ACTIVE_DISTANCE), value -> this.screen.set(NarutoSettings.WORLD_ACTIVE_DISTANCE, value))
                        .number("narutoloading.walls.maxActive", "narutoloading.walls.maxActive.desc", 1, 32, () -> this.screen.settings().get(NarutoSettings.WORLD_MAX_ACTIVE), value -> this.screen.set(NarutoSettings.WORLD_MAX_ACTIVE, value))
                        .toggle("narutoloading.walls.cull", "narutoloading.walls.cull.desc", () -> this.screen.settings().get(NarutoSettings.WORLD_CULL_INVISIBLE), value -> this.screen.set(NarutoSettings.WORLD_CULL_INVISIBLE, value)));

        page.collapsibleCard("narutoloading.walls.audio", "narutoloading.walls.section.audio", "narutoloading.walls.audio.summary", false, null, section -> section
                        .toggle("narutoloading.walls.audioEnabled", "narutoloading.walls.audioEnabled.desc", () -> this.screen.settings().get(NarutoSettings.WORLD_AUDIO_ENABLED), value -> this.screen.set(NarutoSettings.WORLD_AUDIO_ENABLED, value))
                        .slider("narutoloading.walls.audioVolume", "narutoloading.walls.audioVolume.desc", 0.0D, 2.0D, 0.05D, () -> (double) this.screen.settings().get(NarutoSettings.WORLD_VOLUME), value -> this.screen.set(NarutoSettings.WORLD_VOLUME, (float) value), value -> String.format(Locale.ROOT, "%.2f", value))
                        .enabledWhen(() -> this.screen.settings().get(NarutoSettings.WORLD_AUDIO_ENABLED))
                        .number("narutoloading.walls.audioDistance", "narutoloading.walls.audioDistance.desc", 4, 128, () -> this.screen.settings().get(NarutoSettings.WORLD_AUDIO_MAX_DISTANCE), value -> this.screen.set(NarutoSettings.WORLD_AUDIO_MAX_DISTANCE, value))
                        .enabledWhen(() -> this.screen.settings().get(NarutoSettings.WORLD_AUDIO_ENABLED))
                        .number("narutoloading.walls.maxAudio", "narutoloading.walls.maxAudio.desc", 1, 8, () -> this.screen.settings().get(NarutoSettings.WORLD_MAX_AUDIO), value -> this.screen.set(NarutoSettings.WORLD_MAX_AUDIO, value))
                        .enabledWhen(() -> this.screen.settings().get(NarutoSettings.WORLD_AUDIO_ENABLED)));

        if (this.dirty) this.markDirty();
    }

    /**
     * Sends a pending per-wall edit immediately, for example when the screen closes before the
     * debounce fires.
     */
    void destroy() {
        if (this.dirty) this.send();
    }

    private void resolveSelection(@NotNull List<Wall> walls) {
        Wall target = this.selected;

        if (target == null || !walls.contains(target)) target = crosshair(walls);
        if (target == null && !walls.isEmpty()) target = walls.get(0);

        if (target == this.selected) return;

        this.selected = target;
        this.pendingMedia = target == null ? "" : target.media;
        this.pendingVolume = target == null ? 1.0F : target.volume;
        this.pendingLight = target != null && target.light;
        this.dirty = false;
    }

    private void selectWall(@NotNull Wall wall) {
        this.send();

        this.selected = wall;
        this.pendingMedia = wall.media;
        this.pendingVolume = wall.volume;
        this.pendingLight = wall.light;
        this.dirty = false;

        MuiModApi.postToUiThread(this.screen::reloadPage);
    }

    private void markDirty() {
        this.dirty = true;

        int generation = ++this.sendGeneration;
        View view = this.anchor;
        if (view == null) {
            this.send();
            return;
        }

        view.postDelayed(() -> {
            if (generation == this.sendGeneration) this.send();
        }, 400L);
    }

    private void send() {
        Wall wall = this.selected;
        if (wall == null || !this.dirty) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return;

        Wall updated = Wall.fromCorners(wall.toLongArray(), wall.dimension);
        if (updated == null) return;

        updated.media = this.pendingMedia;
        updated.volume = this.pendingVolume;
        updated.light = this.pendingLight;

        this.dirty = false;
        NarutoNetwork.CHANNEL.sendToServer(new WallUpdateMessage(updated));
        this.screen.status(I18n.get("narutoloading.status.wallSaved"));
    }

    private void restart() {
        Wall wall = this.selected;
        if (wall == null) return;

        Minecraft.getInstance().execute(() -> {
            WallTV<?> tv = WallManager.get(wall);
            if (tv != null) tv.synchronize(true, 0D);
        });

        this.screen.status(I18n.get("narutoloading.status.wallRestarted"));
    }

    private void clean() {
        Wall wall = this.selected;
        if (wall == null) return;

        this.send();

        MuiScreenHost host = this.screen.host();
        if (host == null) return;

        MuiDialog.confirm(host,
                I18n.get("narutoloading.walls.clean.title"),
                I18n.get("narutoloading.walls.clean.message"),
                I18n.get("narutoloading.walls.confirm"),
                () -> NarutoNetwork.CHANNEL.sendToServer(new WallCleanRequestMessage(wall)),
                I18n.get("narutoloading.walls.cancel"),
                () -> {});
    }

    private void remove() {
        Wall wall = this.selected;
        if (wall == null) return;

        this.send();

        MuiScreenHost host = this.screen.host();
        if (host == null) return;

        MuiDialog.confirm(host,
                I18n.get("narutoloading.walls.remove.title"),
                I18n.get("narutoloading.walls.remove.message"),
                I18n.get("narutoloading.walls.confirm"),
                () -> {
                    this.selected = null;
                    this.dirty = false;
                    NarutoNetwork.CHANNEL.sendToServer(new WallRemoveRequestMessage(wall));
                },
                I18n.get("narutoloading.walls.cancel"),
                () -> {});
    }

    private static @NotNull List<Wall> walls(@NotNull ClientLevel level) {
        List<Wall> walls = new ArrayList<>();
        for (WallTV<?> tv : WallManager.getIn(level.dimension().location())) walls.add(tv.wall);
        walls.sort(Comparator.comparingDouble(WallManager::distanceTo));
        return walls;
    }

    static int indexOf(@Nullable Wall wall) {
        if (wall == null) return -1;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return -1;

        List<Wall> walls = walls(level);
        return walls.indexOf(wall);
    }

    static @NotNull String describe(@Nullable Wall wall) {
        if (wall == null) return "";

        BlockPos pos = wall.leftBottom;
        int width = wall.faceWidth();
        int height = wall.faceHeight();

        return width + "×" + height + " · " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static @Nullable Wall crosshair(@NotNull List<Wall> walls) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return null;

        float partialTick = minecraft.getPartialTick();
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 view = player.getViewVector(partialTick);

        Wall target = null;
        double best = Double.MAX_VALUE;

        for (Wall wall : walls) {
            Vec3 intersection = WallGeometry.getIntersection(eye, view, wall);
            if (intersection == null) continue;

            double distance = eye.distanceToSqr(intersection);
            if (distance < best) {
                best = distance;
                target = wall;
            }
        }

        return target;
    }

    private static @NotNull String label(@NotNull Wall wall, int index) {
        String media = wall.media.isBlank() ? I18n.get("narutoloading.walls.media.follow") : wall.media;
        return "#" + (index + 1) + " · " + wall.faceWidth() + "×" + wall.faceHeight() + " · " + Math.round(WallManager.distanceTo(wall)) + "m · " + media;
    }
}
