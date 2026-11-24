package me.kall.narutoloading.client.ui;

import icyllis.modernui.core.Context;
import icyllis.modernui.widget.Button;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.config.NarutoSettings;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.core.tool.ToolRegistry;
import me.kall.narutoui.mui.layout.MuiControls;
import me.kall.narutoui.mui.layout.MuiLayout;
import me.kall.narutoui.mui.screen.MuiDialog;
import me.kall.narutoui.mui.screen.MuiScreenHost;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import me.kall.narutoui.mui.screen.MuiSettingsSection;
import me.kall.narutoui.mui.theme.MuiTheme;
import me.kall.narutoui.mui.util.MuiFilePicker;
import net.minecraft.SharedConstants;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

final class GeneralPage implements SettingsPageBuilder {
    private final NarutoScreen screen;
    private final NarutoContext context;

    private @Nullable MuiSettingsSection ffmpegSection;
    private @Nullable MuiSettingsSection ytDlpSection;

    GeneralPage(@NotNull NarutoScreen screen, @NotNull NarutoContext context) {
        this.screen = screen;
        this.context = context;
    }

    @Override
    public void build(@NotNull MuiSettingsPage page) {
        NarutoSettingsState state = this.screen.settings();

        page.shimmerAll()
                .section("narutoloading.general.section.theme", section -> section
                        .choiceGridBound("narutoloading.general.theme", "narutoloading.general.theme.desc", NarutoTheme.options(), () -> NarutoTheme.normalized(state.get(NarutoSettings.THEME)), value -> this.screen.set(NarutoSettings.THEME, value)))
                .section("narutoloading.general.section.shimmer", section -> section
                        .toggle("narutoloading.general.shimmer", "narutoloading.general.shimmer.desc", () -> state.get(NarutoSettings.SHIMMER), value -> this.screen.set(NarutoSettings.SHIMMER, value))
                        .slider("narutoloading.general.shimmerSpeed", "narutoloading.general.shimmerSpeed.desc", 0.2D, 3.0D, 0.1D, () -> state.get(NarutoSettings.SHIMMER_SPEED), value -> this.screen.set(NarutoSettings.SHIMMER_SPEED, value), GeneralPage::format)
                        .enabledWhen(() -> state.get(NarutoSettings.SHIMMER))
                        .slider("narutoloading.general.shimmerScale", "narutoloading.general.shimmerScale.desc", 0.2D, 3.0D, 0.1D, () -> state.get(NarutoSettings.SHIMMER_SCALE), value -> this.screen.set(NarutoSettings.SHIMMER_SCALE, value), GeneralPage::format)
                        .enabledWhen(() -> state.get(NarutoSettings.SHIMMER))
                        .slider("narutoloading.general.shimmerStrength", "narutoloading.general.shimmerStrength.desc", 0.0D, 1.0D, 0.05D, () -> state.get(NarutoSettings.SHIMMER_STRENGTH), value -> this.screen.set(NarutoSettings.SHIMMER_STRENGTH, value), GeneralPage::format)
                        .enabledWhen(() -> state.get(NarutoSettings.SHIMMER))
                        .slider("narutoloading.general.shimmerFps", "narutoloading.general.shimmerFps.desc", 5, 120, () -> state.get(NarutoSettings.SHIMMER_FPS), value -> this.screen.set(NarutoSettings.SHIMMER_FPS, value), Integer::toString)
                        .enabledWhen(() -> state.get(NarutoSettings.SHIMMER)))
                .section("narutoloading.general.section.ffmpeg", section -> {
                    this.ffmpegSection = section;
                    this.fillFfmpeg(section);
                })
                .section("narutoloading.general.section.ytdlp", section -> {
                    this.ytDlpSection = section;
                    this.fillYtDlp(section);
                })
                .section("narutoloading.general.section.advanced", section -> section
                        .toggle("narutoloading.general.debug", "narutoloading.general.debug.desc", () -> state.get(NarutoSettings.DEBUG_TIME_COSTS), value -> this.screen.set(NarutoSettings.DEBUG_TIME_COSTS, value))
                        .number("narutoloading.general.debugInterval", "narutoloading.general.debugInterval.desc", 1, 600, () -> state.get(NarutoSettings.DEBUG_TIME_COSTS_INTERVAL), value -> this.screen.set(NarutoSettings.DEBUG_TIME_COSTS_INTERVAL, value))
                        .enabledWhen(() -> state.get(NarutoSettings.DEBUG_TIME_COSTS))
                        .description("narutoloading.general.configFile", configFile().toString())
                        .button("narutoloading.general.reset", true, this::confirmReset))
                .section("narutoloading.general.section.about", section -> section
                        .value("narutoloading.general.version", modVersion())
                        .value("narutoloading.general.minecraft", SharedConstants.getCurrentVersion().getName())
                        .note("narutoloading.general.shortcuts", "narutoloading.general.shortcuts.desc"));

        page.collapsibleSection("narutoloading.general.faq", "narutoloading.general.faq.desc", false, section -> section
                .note("narutoloading.general.faq.ffmpeg", "narutoloading.general.faq.ffmpeg.desc")
                .note("narutoloading.general.faq.black", "narutoloading.general.faq.black.desc")
                .note("narutoloading.general.faq.wall", "narutoloading.general.faq.wall.desc"));
    }

    private void fillFfmpeg(@NotNull MuiSettingsSection section) {
        String ffmpeg = this.context.tools().ffmpeg.ffmpeg.get();
        String ffprobe = this.context.tools().ffmpeg.ffprobe.get();
        boolean ready = ffmpeg != null;

        section.value("narutoloading.general.ffmpeg.state", I18n.get(ready ? "narutoloading.general.ffmpeg.ready" : "narutoloading.general.ffmpeg.missing"));
        section.description("narutoloading.general.ffmpeg.path", ready ? ffmpeg : I18n.get("narutoloading.general.tool.none"));

        if (ready) {
            section.description("narutoloading.general.ffmpeg.probe", ffprobe == null ? I18n.get("narutoloading.general.tool.none") : ffprobe);
        } else {
            section.note("narutoloading.general.ffmpeg.hint", "narutoloading.general.ffmpeg.hint.desc");
        }

        section.customLayout(row -> {
            Context context = row.getContext();
            MuiTheme theme = row.theme();

            Button choose = MuiControls.button(context, theme, "narutoloading.general.ffmpeg.choose", true, view -> this.chooseFfmpeg());
            Button redetect = MuiControls.button(context, theme, "narutoloading.general.tool.redetect", true, view -> this.redetectFfmpeg());

            row.add(MuiLayout.row(context, theme).padding(0).spacing(8).addWeighted(choose, 1.0F).addWeighted(redetect, 1.0F), MuiControls.fill());
        });
    }

    private void fillYtDlp(@NotNull MuiSettingsSection section) {
        String ytDlp = this.context.tools().ytDlp.ytdlp.get();
        boolean ready = ytDlp != null;

        section.value("narutoloading.general.ytdlp.state", I18n.get(ready ? "narutoloading.general.ytdlp.ready" : "narutoloading.general.ytdlp.missing"));
        section.description("narutoloading.general.ytdlp.path", ready ? ytDlp : I18n.get("narutoloading.general.tool.none"));

        if (!ready) section.note("narutoloading.general.ytdlp.hint", "narutoloading.general.ytdlp.hint.desc");

        section.customLayout(row -> {
            Context context = row.getContext();
            MuiTheme theme = row.theme();

            Button choose = MuiControls.button(context, theme, "narutoloading.general.ytdlp.choose", true, view -> this.chooseYtDlp());
            Button redetect = MuiControls.button(context, theme, "narutoloading.general.tool.redetect", true, view -> this.redetectYtDlp());

            row.add(MuiLayout.row(context, theme).padding(0).spacing(8).addWeighted(choose, 1.0F).addWeighted(redetect, 1.0F), MuiControls.fill());
        });
    }

    private void chooseFfmpeg() {
        MuiFilePicker.openFolder(I18n.get("narutoloading.general.ffmpeg.choose"), path -> {
            this.context.useFfmpegDirectory(path);
            this.refreshFfmpeg();
            this.screen.status(I18n.get("narutoloading.general.ffmpeg.selected"));
        });
    }

    private void redetectFfmpeg() {
        this.context.reloadFFmpeg();
        this.refreshFfmpeg();
        this.screen.status(I18n.get("narutoloading.general.tool.redetected"));
    }

    private void refreshFfmpeg() {
        MuiSettingsSection section = this.ffmpegSection;
        if (section != null) section.rebuild(this::fillFfmpeg);
    }

    private void chooseYtDlp() {
        MuiFilePicker.openFile(I18n.get("narutoloading.general.ytdlp.choose"), "yt-dlp", this::installYtDlp, ToolRegistry.YTDLP_FILE_FILTER);
    }

    private void installYtDlp(@NotNull Path source) {
        if (!Files.isRegularFile(source)) {
            this.screen.status(I18n.get("narutoloading.general.ytdlp.invalid", source.getFileName().toString()));
            return;
        }

        String installed = this.context.tools().installYtDlp(source);

        if (installed == null) {
            this.refreshYtDlp();
            this.screen.status(I18n.get("narutoloading.general.ytdlp.installFailed"));
            return;
        }

        Path parent = source.getParent();
        if (parent != null) this.screen.set(NarutoSettings.YTDLP_DIR, parent.toString());

        this.refreshYtDlp();
        this.screen.status(I18n.get("narutoloading.general.ytdlp.installed", source.getFileName().toString()));
    }

    private void redetectYtDlp() {
        this.context.tools().reloadYtDlp();
        this.refreshYtDlp();
        this.screen.status(I18n.get("narutoloading.general.tool.redetected"));
    }

    private void refreshYtDlp() {
        MuiSettingsSection section = this.ytDlpSection;
        if (section != null) section.rebuild(this::fillYtDlp);
    }

    private void confirmReset() {
        MuiScreenHost host = this.screen.host();
        if (host == null) return;

        MuiDialog.confirm(host,
                I18n.get("narutoloading.general.reset.title"),
                I18n.get("narutoloading.general.reset.message"),
                I18n.get("narutoloading.general.reset.confirm"),
                this.screen::resetDefaults,
                I18n.get("narutoloading.general.reset.cancel"),
                () -> {});
    }

    private static @NotNull String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static @NotNull Path configFile() {
        return FMLPaths.CONFIGDIR.get().resolve("narutoloading.properties");
    }

    private static @NotNull String modVersion() {
        return ModList.get().getModContainerById(NarutoLoading.MOD_ID).map(container -> container.getModInfo().getVersion().toString()).orElse("?");
    }
}
