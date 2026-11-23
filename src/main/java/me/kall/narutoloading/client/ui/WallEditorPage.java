package me.kall.narutoloading.client.ui;

import me.kall.narutoloading.client.ui.util.SaveDebouncer;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.network.NarutoNetwork;
import me.kall.narutoloading.network.message.WallCleanRequestMessage;
import me.kall.narutoloading.network.message.WallRemoveRequestMessage;
import me.kall.narutoloading.network.message.WallUpdateMessage;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoloading.wall.client.WallManager;
import me.kall.narutoloading.wall.client.WallTV;
import me.kall.narutoloading.wall.client.light.WallLightHook;
import me.kall.narutoui.mui.choice.MuiOption;
import me.kall.narutoui.mui.screen.MuiDialog;
import me.kall.narutoui.mui.screen.MuiScreenHost;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class WallEditorPage implements SettingsPageBuilder {
    private final NarutoScreen screen;
    private final Wall wall;

    private String media;
    private float volume;
    private boolean light;
    private boolean dirty;

    private final SaveDebouncer saver = new SaveDebouncer();

    WallEditorPage(@NotNull NarutoScreen screen, @NotNull Wall wall) {
        this.screen = screen;
        this.wall = wall;
        this.media = wall.media;
        this.volume = wall.volume;
        this.light = wall.light;
    }

    @NotNull String title() {
        int index = WallsPage.indexOf(this.wall);
        return index < 0 ? I18n.get("narutoloading.wall.title.plain") : I18n.get("narutoloading.wall.title", index + 1);
    }

    @NotNull String subtitle() {
        return WallsPage.describe(this.wall);
    }

    @Override
    public void build(@NotNull MuiSettingsPage page) {
        page.shimmerAll();

        this.saver.anchor(page.context().content());

        List<MuiOption<String>> options = new ArrayList<>();
        options.add(MuiOption.of("", I18n.get("narutoloading.wall.media.follow")));

        for (MediaRepository.Media entry : NarutoContext.get().media().all()) options.add(MuiOption.of(entry.name(), entry.name()));

        page.section("narutoloading.wall.section.media", section -> section
                        .dropdown("narutoloading.wall.media", "narutoloading.wall.media.desc", options, this.media, value -> {
                            this.media = value;
                            this.markDirty();
                        })
                        .note("narutoloading.wall.media.note", "narutoloading.wall.media.note.desc"))
                .section("narutoloading.wall.section.sound", section -> {
                    section.slider("narutoloading.wall.volume", "narutoloading.wall.volume.desc", 0, 200, () -> Math.round(this.volume * 100.0F), value -> {
                                this.volume = value / 100.0F;
                                this.markDirty();
                            }, value -> value + "%")
                            .toggle("narutoloading.wall.light", "narutoloading.wall.light.desc", () -> this.light, value -> {
                                this.light = value;
                                this.markDirty();
                            })
                            .enabledWhen(WallLightHook::active);

                    if (!WallLightHook.active()) section.note("narutoloading.wall.light.unavailable", "narutoloading.wall.light.unavailable.desc");
                })
                .section("narutoloading.wall.section.actions", section -> section
                        .button("narutoloading.wall.restart", true, this::restart)
                        .button("narutoloading.wall.clean", false, this::clean)
                        .button("narutoloading.wall.remove", false, this::remove));

        if (this.dirty) this.markDirty();
    }

    void destroy() {
        if (this.dirty) this.send();
    }

    private void markDirty() {
        this.dirty = true;
        this.saver.schedule(this::send);
    }

    private void send() {
        if (!this.dirty) return;
        if (Minecraft.getInstance().getConnection() == null) return;

        Wall updated = Wall.fromCorners(this.wall.toLongArray(), this.wall.dimension);
        if (updated == null) return;

        updated.media = this.media;
        updated.volume = this.volume;
        updated.light = this.light;

        this.dirty = false;
        NarutoNetwork.CHANNEL.sendToServer(new WallUpdateMessage(updated));
        this.screen.status(I18n.get("narutoloading.status.wallSaved"));
    }

    private void restart() {
        Minecraft.getInstance().execute(() -> {
            WallTV<?> tv = WallManager.get(this.wall);
            if (tv != null) tv.synchronize(true, 0D);
        });

        this.screen.status(I18n.get("narutoloading.status.wallRestarted"));
    }

    private void clean() {
        this.send();

        MuiScreenHost host = this.screen.host();
        if (host == null) return;

        MuiDialog.confirm(host,
                I18n.get("narutoloading.wall.clean.title"),
                I18n.get("narutoloading.wall.clean.message"),
                I18n.get("narutoloading.wall.confirm"),
                () -> NarutoNetwork.CHANNEL.sendToServer(new WallCleanRequestMessage(this.wall)),
                I18n.get("narutoloading.wall.cancel"),
                () -> {});
    }

    private void remove() {
        this.send();

        MuiScreenHost host = this.screen.host();
        if (host == null) return;

        MuiDialog.confirm(host,
                I18n.get("narutoloading.wall.remove.title"),
                I18n.get("narutoloading.wall.remove.message"),
                I18n.get("narutoloading.wall.confirm"),
                () -> {
                    this.dirty = false;
                    NarutoNetwork.CHANNEL.sendToServer(new WallRemoveRequestMessage(this.wall));
                    MuiScreenHost current = this.screen.host();
                    if (current != null) current.close();
                },
                I18n.get("narutoloading.wall.cancel"),
                () -> {});
    }
}
