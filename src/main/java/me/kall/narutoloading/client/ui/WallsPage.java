package me.kall.narutoloading.client.ui;

import icyllis.modernui.mc.MuiModApi;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.network.NarutoNetwork;
import me.kall.narutoloading.network.message.WallCleanRequestMessage;
import me.kall.narutoloading.network.message.WallRemoveRequestMessage;
import me.kall.narutoloading.network.message.WallUpdateMessage;
import me.kall.narutoloading.wall.geometry.WallGeometry;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.client.WallManager;
import me.kall.narutoloading.wall.client.WallTV;
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

final class WallsPage implements SettingsPageBuilder {
    private final NarutoScreen screen;

    private @Nullable Wall selected;
    private String pendingMedia = "";
    private float pendingVolume = 1.0F;
    private boolean pendingLight;

    WallsPage(@NotNull NarutoScreen screen) {
        this.screen = screen;
    }

    @Override
    public void build(@NotNull MuiSettingsPage page) {
        page.shimmerAll();

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || minecraft.getConnection() == null || !NarutoNetwork.CHANNEL.isRemotePresent(minecraft.getConnection().getConnection())) {
            page.section("narutoloading.walls.section.target", section -> section
                    .note("narutoloading.walls.unavailable", "narutoloading.walls.unavailable.desc"));
            return;
        }

        List<Wall> walls = new ArrayList<>();
        for (WallTV<?> tv : WallManager.getIn(level.dimension().location())) walls.add(tv.wall);
        walls.sort(Comparator.comparingDouble(WallManager::distanceTo));

        this.resolveSelection(walls);

        page.section("narutoloading.walls.section.target", section -> {
            List<MuiOption<Wall>> options = new ArrayList<>(walls.size());
            for (int index = 0; index < walls.size(); index++) options.add(MuiOption.of(walls.get(index), label(walls.get(index), index)));

            section.dropdown("narutoloading.walls.target", "narutoloading.walls.target.desc", options, this.selected, this::selectWall);

            if (walls.isEmpty()) section.note("narutoloading.walls.empty", "narutoloading.walls.empty.desc");
        });

        Wall wall = this.selected;
        if (wall == null) return;

        page.section("narutoloading.walls.section.settings", section -> {
            List<MuiOption<String>> options = new ArrayList<>();
            options.add(MuiOption.of("", I18n.get("narutoloading.walls.media.current")));

            for (MediaRepository.Media media : NarutoContext.get().media().all()) options.add(MuiOption.of(media.name(), media.name()));

            section.dropdown("narutoloading.walls.media", "narutoloading.walls.media.desc", options, this.pendingMedia, value -> this.pendingMedia = value)
                    .slider("narutoloading.walls.volume", "narutoloading.walls.volume.desc", 0, 200, () -> Math.round(this.pendingVolume * 100.0F), value -> this.pendingVolume = value / 100.0F, value -> value + "%")
                    .toggle("narutoloading.walls.light", "narutoloading.walls.light.desc", () -> this.pendingLight, value -> this.pendingLight = value);
        });

        page.section("narutoloading.walls.section.actions", section -> section
                .button("narutoloading.walls.apply", true, this::apply)
                .button("narutoloading.walls.restart", true, this::restart)
                .button("narutoloading.walls.clean", false, this::clean)
                .button("narutoloading.walls.remove", false, this::remove));
    }

    private void resolveSelection(@NotNull List<Wall> walls) {
        Wall focus = this.screen.focusWall();
        Wall target = focus != null ? focus : this.selected;

        if (target == null || !walls.contains(target)) target = crosshair(walls);
        if (target == null && !walls.isEmpty()) target = walls.get(0);

        if (target == this.selected) return;

        this.selected = target;
        this.pendingMedia = target == null ? "" : target.media;
        this.pendingVolume = target == null ? 1.0F : target.volume;
        this.pendingLight = target != null && target.light;
    }

    private void selectWall(@NotNull Wall wall) {
        this.selected = wall;
        this.pendingMedia = wall.media;
        this.pendingVolume = wall.volume;
        this.pendingLight = wall.light;

        MuiModApi.postToUiThread(this.screen::reloadPage);
    }

    private void apply() {
        Wall wall = this.selected;
        if (wall == null) return;

        Wall updated = Wall.fromCorners(wall.toLongArray(), wall.dimension);
        if (updated == null) return;

        updated.media = this.pendingMedia;
        updated.volume = this.pendingVolume;
        updated.light = this.pendingLight;

        NarutoNetwork.CHANNEL.sendToServer(new WallUpdateMessage(updated));
    }

    private void restart() {
        Wall wall = this.selected;
        if (wall == null) return;

        Minecraft.getInstance().execute(() -> {
            WallTV<?> tv = WallManager.get(wall);
            if (tv != null) tv.synchronize(true, 0D);
        });
    }

    private void clean() {
        Wall wall = this.selected;
        if (wall == null) return;

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

        MuiScreenHost host = this.screen.host();
        if (host == null) return;

        MuiDialog.confirm(host,
                I18n.get("narutoloading.walls.remove.title"),
                I18n.get("narutoloading.walls.remove.message"),
                I18n.get("narutoloading.walls.confirm"),
                () -> NarutoNetwork.CHANNEL.sendToServer(new WallRemoveRequestMessage(wall)),
                I18n.get("narutoloading.walls.cancel"),
                () -> {});
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
        int width = wall.faceWidth();
        int height = wall.faceHeight();

        BlockPos pos = wall.leftBottom;
        String media = wall.media.isBlank() ? I18n.get("narutoloading.walls.media.current") : wall.media;

        return "#" + (index + 1) + " (" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ") " + width + "×" + height + " · " + media;
    }
}
