package me.kall.narutoloading.client.ui;

import icyllis.modernui.core.Context;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import me.kall.narutoloading.config.NarutoSettings;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.client.ClientUiControl;
import me.kall.narutoloading.media.MediaNaming;
import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoui.mui.choice.MuiOption;
import me.kall.narutoui.mui.layout.MuiControls;
import me.kall.narutoui.mui.layout.MuiLayout;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import me.kall.narutoui.mui.theme.MuiTheme;
import me.kall.narutoui.mui.util.MuiFilePicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class MediaPage implements SettingsPageBuilder {
    private final NarutoScreen screen;
    private final NarutoContext context;
    private final DownloadCard download;

    MediaPage(@NotNull NarutoScreen screen, @NotNull NarutoContext context, @Nullable String link) {
        this.screen = screen;
        this.context = context;
        this.download = new DownloadCard(screen, context, link);
    }

    @Override
    public void build(@NotNull MuiSettingsPage page) {
        MediaRepository media = this.context.media();
        MediaRepository.Media current = media.current();
        List<MediaRepository.Media> all = media.all();

        page.shimmerAll()
                .section("narutoloading.media.section.current", section -> {
                    String count = all.isEmpty() && media.scanning()
                            ? I18n.get("narutoloading.media.count.scanning")
                            : Integer.toString(all.size());

                    section.description("narutoloading.media.current", current == null ? I18n.get("narutoloading.media.current.none") : current.name())
                            .value("narutoloading.media.count", count);

                    if (!all.isEmpty()) {
                        List<MuiOption<String>> options = new ArrayList<>(all.size());
                        for (MediaRepository.Media entry : all) options.add(MuiOption.of(entry.name(), entry.name()));

                        section.dropdown("narutoloading.media.pick", "narutoloading.media.pick.desc", options, current == null ? null : current.name(), this::select);
                    }

                    section.customLayout(row -> {
                        Context context = row.getContext();
                        MuiTheme theme = row.theme();

                        Button next = MuiControls.button(context, theme, "narutoloading.media.next", true, view -> this.next());
                        Button restart = MuiControls.button(context, theme, "narutoloading.media.restart", true, view -> this.restart());
                        Button importMedia = MuiControls.button(context, theme, "narutoloading.media.import", true, view -> this.importMedia());
                        Button open = MuiControls.button(context, theme, "narutoloading.media.open", true, view -> this.openFolder());

                        row.add(MuiLayout.row(context, theme).padding(0).spacing(8).addWeighted(next, 1.0F).addWeighted(restart, 1.0F), MuiControls.fill());

                        LinearLayout.LayoutParams second = MuiControls.fill();
                        second.topMargin = MuiControls.dp(context, 8);
                        row.add(MuiLayout.row(context, theme).padding(0).spacing(8).addWeighted(importMedia, 1.0F).addWeighted(open, 1.0F), second);
                    });

                    section.note("narutoloading.media.note", "narutoloading.media.note.desc");
                });

        this.download.build(page);

        page.section("narutoloading.media.section.folder", section -> section
                .textField("narutoloading.media.folder", "narutoloading.media.folder.desc", this.screen.settings().get(NarutoSettings.SOURCES_DIR), value -> {
                    this.screen.set(NarutoSettings.SOURCES_DIR, value);
                    this.screen.reloadSources();
                    MuiModApi.postToUiThread(this.screen::reloadPage);
                }, field -> field.onChange(value -> this.screen.set(NarutoSettings.SOURCES_DIR, value)))
                .button("narutoloading.media.folder.choose", true, this::chooseFolder)
                .description("narutoloading.media.folder.resolved", this.folder().toString())
                .note("narutoloading.media.folder.note", "narutoloading.media.folder.note.desc")
                .note("narutoloading.media.folder.formats", "narutoloading.media.folder.formats.desc"));

        page.section("narutoloading.media.section.pick", section -> section
                .dropdownBound("narutoloading.media.mode", "narutoloading.media.mode.desc", modes(), () -> this.screen.settings().get(NarutoSettings.SELECT_MODE), value -> this.screen.set(NarutoSettings.SELECT_MODE, value))
                .textField("narutoloading.media.fixedName", "narutoloading.media.fixedName.desc", this.screen.settings().get(NarutoSettings.FIXED_NAME), value -> this.screen.set(NarutoSettings.FIXED_NAME, value), field -> field.onChange(value -> this.screen.set(NarutoSettings.FIXED_NAME, value)))
                .enabledWhen(() -> this.screen.settings().get(NarutoSettings.SELECT_MODE) == MediaRepository.Mode.FIXED));
    }

    void destroy() {
        this.download.destroy();
    }

    private void select(@NotNull String name) {
        this.switchMedia(() -> this.context.media().select(name));
    }

    private void next() {
        this.switchMedia(() -> this.context.media().advance());
    }

    private void restart() {
        this.switchMedia(() -> {});
    }

    private void switchMedia(@NotNull Runnable selection) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            selection.run();
            ClientUiControl.restart(0D);
            MuiModApi.postToUiThread(this.screen::reloadPage);
        });
    }

    private void importMedia() {
        MuiFilePicker.openFile(I18n.get("narutoloading.media.import"), I18n.get("narutoloading.media.import.filter"), this::installMedia, MediaNaming.fileFilters());
    }

    private void installMedia(@NotNull Path source) {
        this.context.media().importMedia(source).whenComplete((target, throwable) -> {
            if (throwable != null) System.err.println("[NarutoLoading] Exception importing " + source + ": " + throwable.getMessage());

            if (target == null) {
                this.screen.notifyImport(I18n.get("narutoloading.media.import.failed"));
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
                    this.screen.notifyImport(I18n.get("narutoloading.media.import.unplayable", name));
                    return;
                }

                this.screen.reloadPage();
            });
        });
    }

    private void chooseFolder() {
        MuiFilePicker.openFolder(I18n.get("narutoloading.media.folder.choose"), path -> {
            this.screen.set(NarutoSettings.SOURCES_DIR, path.toString());
            this.screen.reloadSources();
            this.screen.reloadPage();
        });
    }

    private void openFolder() {
        Folders.open(this.folder());
    }

    private @NotNull Path folder() {
        return this.screen.sourcesFolder();
    }

    @Contract(" -> new")
    private static @NotNull @Unmodifiable List<MuiOption<MediaRepository.Mode>> modes() {
        return List.of(
                MuiOption.of(MediaRepository.Mode.RANDOM, I18n.get("narutoloading.media.mode.random")),
                MuiOption.of(MediaRepository.Mode.SEQUENTIAL, I18n.get("narutoloading.media.mode.sequential")),
                MuiOption.of(MediaRepository.Mode.FIXED, I18n.get("narutoloading.media.mode.fixed")));
    }
}
