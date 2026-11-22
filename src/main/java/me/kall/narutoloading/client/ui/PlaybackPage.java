package me.kall.narutoloading.client.ui;

import icyllis.modernui.core.Context;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.widget.Button;
import me.kall.narutoloading.media.MediaNaming;
import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.config.NarutoSettings;
import me.kall.narutoloading.client.ClientUiControl;
import me.kall.narutoui.mui.choice.MuiOption;
import me.kall.narutoui.mui.layout.MuiControls;
import me.kall.narutoui.mui.layout.MuiLayout;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import me.kall.narutoui.mui.screen.MuiSettingsSection;
import me.kall.narutoui.mui.theme.MuiTheme;
import me.kall.narutoui.mui.util.MuiFilePicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class PlaybackPage implements SettingsPageBuilder {
    private final NarutoScreen screen;
    private final NarutoContext context;

    private @Nullable MuiSettingsSection ffmpegSection;

    PlaybackPage(@NotNull NarutoScreen screen, @NotNull NarutoContext context) {
        this.screen = screen;
        this.context = context;
    }

    @Override
    public void build(@NotNull MuiSettingsPage page) {
        MediaRepository media = this.context.media();
        MediaRepository.Media current = media.current();
        List<MediaRepository.Media> all = media.all();

        page.shimmerAll()
                .section("narutoloading.playback.section.current", section -> {
                    section.value("narutoloading.playback.current", current == null ? I18n.get("narutoloading.playback.current.none") : current.name())
                            .value("narutoloading.playback.count", Integer.toString(all.size()))
                            .button("narutoloading.playback.import", true, this::importMedia);

                    if (!all.isEmpty()) {
                        List<MuiOption<String>> options = new ArrayList<>(all.size());
                        for (MediaRepository.Media entry : all) options.add(MuiOption.of(entry.name(), entry.name()));

                        section.dropdown("narutoloading.playback.pick", "narutoloading.playback.pick.desc", options, current == null ? null : current.name(), this::selectMedia);
                    }

                    section.button("narutoloading.playback.next", false, this::nextMedia)
                            .button("narutoloading.playback.restart", true, this::restartMedia)
                            .note("narutoloading.playback.note", "narutoloading.playback.note.desc");
                })

                .section("narutoloading.playback.section.audio", section -> section
                        .slider("narutoloading.playback.volume", "narutoloading.playback.volume.desc", 0, 100, () -> Math.round(this.screen.draft().get(NarutoSettings.AUDIO_VOLUME) * 100.0F), value -> this.screen.edit(() -> {
                            this.screen.draft().set(NarutoSettings.AUDIO_VOLUME, value / 100.0F);
                            this.screen.applyVolume();
                        }), value -> value + "%")
                        .toggle("narutoloading.playback.mute", "narutoloading.playback.mute.desc", () -> this.screen.draft().get(NarutoSettings.MUTE_MUSIC), value -> this.screen.edit(() -> this.screen.draft().set(NarutoSettings.MUTE_MUSIC, value))))
                .section("narutoloading.playback.section.ffmpeg", section -> {
                    this.ffmpegSection = section;
                    this.fillFfmpeg(section);
                });
    }

    private void fillFfmpeg(@NotNull MuiSettingsSection section) {
        String ffmpeg = this.context.tools().ffmpeg.ffmpeg.get();
        String ffprobe = this.context.tools().ffmpeg.ffprobe.get();

        section.value("narutoloading.playback.ffmpeg", ffmpeg == null ? I18n.get("narutoloading.playback.ffmpeg.none") : ffmpeg)
                .value("narutoloading.playback.ffprobe", ffprobe == null ? I18n.get("narutoloading.playback.ffmpeg.none") : ffprobe)
                .customLayout(row -> {
                    Context context = row.getContext();
                    MuiTheme theme = row.theme();

                    Button choose = MuiControls.button(context, theme, "narutoloading.playback.ffmpeg.choose", true, view -> this.chooseFfmpegFolder());
                    Button redetect = MuiControls.button(context, theme, "narutoloading.playback.ffmpeg.redetect", true, view -> this.redetectFfmpeg());

                    row.add(MuiLayout.row(context, theme).padding(0).spacing(8).addWeighted(choose, 1.0F).addWeighted(redetect, 1.0F), MuiControls.fill());
                })
                .note("narutoloading.playback.ffmpeg.note", "narutoloading.playback.ffmpeg.note.desc");
    }

    private void chooseFfmpegFolder() {
        MuiFilePicker.openFolder(I18n.get("narutoloading.playback.ffmpeg.choose"), path -> {
            this.context.useFfmpegDirectory(path);
            this.refreshFfmpeg();
        });
    }

    private void redetectFfmpeg() {
        this.context.reloadFFmpeg();
        this.refreshFfmpeg();
    }

    private void refreshFfmpeg() {
        MuiSettingsSection section = this.ffmpegSection;
        if (section != null) section.rebuild(this::fillFfmpeg);
    }

    private void selectMedia(@NotNull String name) {
        this.switchPlayback(() -> this.context.media().select(name));
    }

    private void nextMedia() {
        this.switchPlayback(() -> this.context.media().advance());
    }

    private void restartMedia() {
        this.switchPlayback(() -> {});
    }

    private void switchPlayback(@NotNull Runnable selection) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            selection.run();
            ClientUiControl.restart(0D);
            MuiModApi.postToUiThread(this.screen::reloadPage);
        });
    }

    private void importMedia() {
        MuiFilePicker.openFile(I18n.get("narutoloading.playback.import"), I18n.get("narutoloading.playback.import.filter"), this::installMedia, MediaNaming.fileFilters());
    }

    private void installMedia(@NotNull Path source) {
        this.context.media().importMedia(source).whenComplete((target, throwable) -> {
            if (throwable != null) System.err.println("[NarutoLoading] Exception importing " + source + ": " + throwable.getMessage());

            if (target == null) {
                this.screen.notifyImport(I18n.get("narutoloading.playback.import.failed.desc"));
                return;
            }

            String name = MediaNaming.mediaName(target.getFileName().toString());
            this.context.media().refresh(() -> this.selectImported(name));
        });
    }

    private void selectImported(@NotNull String name) {
        Minecraft.getInstance().execute(() -> {
            boolean selected = this.context.media().select(name);
            if (selected) ClientUiControl.restart(0D);

            MuiModApi.postToUiThread(() -> {
                if (!selected) {
                    this.screen.notifyImport(I18n.get("narutoloading.playback.import.unplayable", name));
                    return;
                }

                this.screen.reloadPage();
            });
        });
    }
}
