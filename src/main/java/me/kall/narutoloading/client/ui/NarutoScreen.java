package me.kall.narutoloading.client.ui;

import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import me.kall.narutoloading.app.Instance;
import me.kall.narutoloading.app.MediaRepository;
import me.kall.narutoloading.client.fade.FadeCenter;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.impl.ClientUiControl;
import me.kall.narutoloading.produce.audio.AudioProducer;
import me.kall.narutoui.mui.choice.MuiOption;
import me.kall.narutoui.mui.color.MuiColorSchemes;
import me.kall.narutoui.mui.factory.MuiWidgetFactory;
import me.kall.narutoui.mui.layout.MuiControls;
import me.kall.narutoui.mui.layout.MuiLayout;
import me.kall.narutoui.mui.screen.MuiDialog;
import me.kall.narutoui.mui.screen.MuiScreenHost;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import me.kall.narutoui.mui.screen.MuiSettingsScreen;
import me.kall.narutoui.mui.screen.MuiSettingsSection;
import me.kall.narutoui.mui.shimmer.MuiShimmer;
import me.kall.narutoui.mui.shimmer.MuiShimmerConfig;
import me.kall.narutoui.mui.shimmer.MuiShimmerProfile;
import me.kall.narutoui.mui.shimmer.MuiShimmerable;
import me.kall.narutoui.mui.theme.MuiTheme;
import me.kall.narutoui.mui.util.MuiFilePicker;
import me.kall.narutoui.mui.widget.MuiHintBadge;
import me.kall.narutoui.mui.widget.MuiLogo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NarutoScreen {
    private final YtDlpPage downloads;
    private final MuiShimmerProfile shimmerProfile = MuiShimmerProfile.create(new ScreenShimmerConfig());

    private NarutoDraft draft = NarutoDraft.load();

    private @Nullable MuiScreenHost host;
    private @Nullable Button applyButton;
    private @Nullable Button revertButton;
    private @Nullable Button doneButton;
    private @Nullable TextView statusView;
    private @Nullable MuiSettingsSection ffmpegSection;

    private boolean applying;
    private boolean closeDialogOpen;

    private NarutoScreen(@Nullable String link) {
        this.downloads = new YtDlpPage(link);
        Instance.get().media.refresh();
    }

    public static void register(@NotNull FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> create(parent)));
    }

    public static @NotNull Screen create(@Nullable Screen parent) {
        return create(parent, null);
    }

    public static @NotNull Screen create(@Nullable Screen parent, @Nullable String link) {
        return new NarutoScreen(link).build(parent);
    }

    private @NotNull Screen build(@Nullable Screen parent) {
        FadeCenter.preview(() -> this.draft.fadable, () -> this.draft.ticksBeforeFade,
                () -> this.draft.fadeOutStep, () -> this.draft.fadeInStep);

        return MuiSettingsScreen.builder("NarutoLoading")
                .subtitle("narutoloading.screen.subtitle")
                .widgetFactory(MuiWidgetFactory.shimmer())
                .shimmerProfile(this.shimmerProfile)
                .shimmerNavigation()
                .shimmerTitles()
                .railHeader((context, theme) -> MuiLogo.create(context, theme, "N"))
                .railFooter((context, theme) -> MuiHintBadge.create(context, theme, "narutoloading.ytdlp.footer"))
                .actionBar(this::buildActionBar)
                .onCreated(fragment -> {
                    this.host = fragment;
                    this.refreshActions();
                })
                .onDestroyed(fragment -> this.onDestroyed())
                .onCloseRequested(this::onCloseRequested)
                .page("narutoloading.nav.download", "narutoloading.ytdlp.download.title", "narutoloading.ytdlp.download.subtitle", this.downloads::build)
                .page("narutoloading.nav.playback", "narutoloading.playback.title", "narutoloading.playback.subtitle", this::buildPlayback)
                .page("narutoloading.nav.config", "narutoloading.config.title", "narutoloading.config.subtitle", this::buildConfig)
                .page("narutoloading.nav.help", "narutoloading.help.title", "narutoloading.help.subtitle", HelpPage::build)
                .create(parent);
    }

    private void buildPlayback(@NotNull MuiSettingsPage page) {
        page.shimmerAll();

        MediaRepository media = Instance.get().media;
        MediaRepository.Media current = media.current();
        List<MediaRepository.Media> all = media.all();

        page.section("narutoloading.playback.section.current", section -> {
            section.value("narutoloading.playback.current", current == null ? I18n.get("narutoloading.playback.current.none") : current.name());
            section.value("narutoloading.playback.count", Integer.toString(all.size()));
            section.button("narutoloading.playback.import", true, this::importMedia);

            if (!all.isEmpty()) {
                List<MuiOption<String>> options = new ArrayList<>(all.size());
                for (MediaRepository.Media entry : all) options.add(MuiOption.of(entry.name(), entry.name()));

                section.dropdown("narutoloading.playback.pick", "narutoloading.playback.pick.desc", options,
                        current == null ? null : current.name(), this::selectMedia);
            }

            section.button("narutoloading.playback.next", false, this::nextMedia);
            section.button("narutoloading.playback.restart", true, this::restartMedia);
            section.note("narutoloading.playback.note", "narutoloading.playback.note.desc");
        });

        page.section("narutoloading.playback.section.audio", section -> section
                .slider("narutoloading.playback.volume", "narutoloading.playback.volume.desc", 0, 100,
                        () -> Math.round(this.draft.volume * 100.0F),
                        value -> this.edit(() -> {
                            this.draft.volume = value / 100.0F;
                            this.applyVolume();
                        }),
                        value -> value + "%")
                .toggle("narutoloading.playback.mute", "narutoloading.playback.mute.desc",
                        () -> this.draft.muteMusic,
                        value -> this.edit(() -> this.draft.muteMusic = value)));

        page.section("narutoloading.playback.section.ffmpeg", section -> {
            this.ffmpegSection = section;
            this.fillFfmpeg(section);
        });
    }

    private void fillFfmpeg(@NotNull MuiSettingsSection section) {
        String ffmpeg = Instance.get().ffmpeg.ffmpeg.get();
        String ffprobe = Instance.get().ffmpeg.ffprobe.get();

        section.value("narutoloading.playback.ffmpeg", ffmpeg == null ? I18n.get("narutoloading.playback.ffmpeg.none") : ffmpeg)
                .value("narutoloading.playback.ffprobe", ffprobe == null ? I18n.get("narutoloading.playback.ffmpeg.none") : ffprobe);

        section.customLayout(row -> {
            Context context = row.getContext();
            MuiTheme theme = row.theme();

            Button choose = MuiControls.button(context, theme, "narutoloading.playback.ffmpeg.choose", true, view -> this.chooseFfmpegFolder());
            Button redetect = MuiControls.button(context, theme, "narutoloading.playback.ffmpeg.redetect", true, view -> this.redetectFfmpeg());

            row.add(MuiLayout.row(context, theme).padding(0).spacing(8).addWeighted(choose, 1.0F).addWeighted(redetect, 1.0F), MuiControls.fill());
        });

        section.note("narutoloading.playback.ffmpeg.note", "narutoloading.playback.ffmpeg.note.desc");
    }

    private void chooseFfmpegFolder() {
        MuiFilePicker.openFolder(I18n.get("narutoloading.playback.ffmpeg.choose"), path -> {
            NarutoConfig.set(NarutoConfig.FFMPEG_DIR, path.toString());
            NarutoConfig.save();
            Instance.get().reloadFFmpeg();
            this.refreshFfmpeg();
        });
    }

    private void redetectFfmpeg() {
        Instance.get().reloadFFmpeg();
        this.refreshFfmpeg();
    }

    private void refreshFfmpeg() {
        MuiSettingsSection section = this.ffmpegSection;
        if (section != null) section.rebuild(this::fillFfmpeg);
    }

    private void selectMedia(@NotNull String name) {
        this.switchPlayback(() -> Instance.get().media.select(name));
    }

    private void nextMedia() {
        this.switchPlayback(() -> Instance.get().media.advance());
    }

    private void restartMedia() {
        this.switchPlayback(() -> {});
    }

    private void switchPlayback(@NotNull Runnable selection) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            selection.run();
            ClientUiControl.restart(0D);
            MuiModApi.postToUiThread(this::reloadPage);
        });
    }

    private void reloadPage() {
        MuiScreenHost current = this.host;
        if (current != null) current.reloadCurrentPage();
    }

    private void importMedia() {
        MuiFilePicker.openFile(I18n.get("narutoloading.playback.import"), I18n.get("narutoloading.playback.import.filter"), this::installMedia, MediaRepository.fileFilters());
    }

    private void installMedia(@NotNull Path source) {
        Instance instance = Instance.get();

        instance.importMedia(source).whenComplete((target, throwable) -> {
            if (throwable != null) System.err.println("[NarutoLoading] Exception importing " + source + ": " + throwable.getMessage());

            if (target == null) {
                this.notifyImport(I18n.get("narutoloading.playback.import.failed.desc"));
                return;
            }

            String name = MediaRepository.mediaName(target.getFileName().toString());
            instance.media.refresh(() -> this.selectImported(name));
        });
    }

    private void selectImported(@NotNull String name) {
        Minecraft.getInstance().execute(() -> {
            boolean selected = Instance.get().media.select(name);
            if (selected) ClientUiControl.restart(0D);

            MuiModApi.postToUiThread(() -> {
                if (!selected) {
                    this.notifyImport(I18n.get("narutoloading.playback.import.unplayable", name));
                    return;
                }

                this.reloadPage();
            });
        });
    }

    private void notifyImport(@NotNull String message) {
        MuiModApi.postToUiThread(() -> {
            MuiScreenHost current = this.host;
            if (current != null) MuiDialog.notice(current, I18n.get("narutoloading.playback.import.title"), message, I18n.get("narutoloading.ok"), () -> {});
        });
    }

    private void buildConfig(@NotNull MuiSettingsPage page) {
        page.shimmerAll();

        page.section("narutoloading.config.section.video", section -> section
                        .number("narutoloading.config.width", "narutoloading.config.width.desc", 16, 7680,
                                () -> this.draft.maxVideoWidth,
                                value -> this.edit(() -> this.draft.maxVideoWidth = value))
                        .number("narutoloading.config.height", "narutoloading.config.height.desc", 16, 4320,
                                () -> this.draft.maxVideoHeight,
                                value -> this.edit(() -> this.draft.maxVideoHeight = value))
                        .decimal("narutoloading.config.buffer", "narutoloading.config.buffer.desc", 0.0D, 5.0D,
                                () -> this.draft.bufferSeconds,
                                value -> this.edit(() -> this.draft.bufferSeconds = value))
                        .toggle("narutoloading.config.compatMode", "narutoloading.config.compatMode.desc",
                                () -> this.draft.compatMode,
                                value -> this.edit(() -> this.draft.compatMode = value)))
                .section("narutoloading.config.section.appearance", section -> section
                        .toggle("narutoloading.config.shimmer", "narutoloading.config.shimmer.desc",
                                () -> this.draft.shimmer,
                                value -> {
                                    this.edit(() -> this.draft.shimmer = value);
                                    MuiShimmer.refresh();
                                })
                        .slider("narutoloading.config.shimmerSpeed", "narutoloading.config.shimmerSpeed.desc", 0.2D, 3.0D, 0.1D,
                                () -> this.draft.shimmerSpeed,
                                value -> this.edit(() -> this.draft.shimmerSpeed = value),
                                value -> String.format(Locale.ROOT, "%.1f", value))
                        .enabledWhen(() -> this.draft.shimmer)
                        .slider("narutoloading.config.shimmerScale", "narutoloading.config.shimmerScale.desc", 0.2D, 3.0D, 0.1D,
                                () -> this.draft.shimmerScale,
                                value -> this.edit(() -> this.draft.shimmerScale = value),
                                value -> String.format(Locale.ROOT, "%.1f", value))
                        .enabledWhen(() -> this.draft.shimmer)
                        .slider("narutoloading.config.shimmerStrength", "narutoloading.config.shimmerStrength.desc", 0.0D, 1.0D, 0.05D,
                                () -> this.draft.shimmerStrength,
                                value -> this.edit(() -> this.draft.shimmerStrength = value),
                                value -> String.format(Locale.ROOT, "%.2f", value))
                        .enabledWhen(() -> this.draft.shimmer)
                        .slider("narutoloading.config.shimmerFps", "narutoloading.config.shimmerFps.desc", 5, 120,
                                () -> this.draft.shimmerFps,
                                value -> this.edit(() -> this.draft.shimmerFps = value),
                                Integer::toString)
                        .enabledWhen(() -> this.draft.shimmer)
                        .toggle("narutoloading.config.fade", "narutoloading.config.fade.desc",
                                () -> this.draft.fadable,
                                value -> this.edit(() -> this.draft.fadable = value))
                        .number("narutoloading.config.fadeTicks", "narutoloading.config.fadeTicks.desc", 0, 1200,
                                () -> this.draft.ticksBeforeFade,
                                value -> this.edit(() -> this.draft.ticksBeforeFade = value))
                        .number("narutoloading.config.fadeOutStep", "narutoloading.config.fadeOutStep.desc", 0, 100,
                                () -> this.draft.fadeOutStep,
                                value -> this.edit(() -> this.draft.fadeOutStep = value))
                        .number("narutoloading.config.fadeInStep", "narutoloading.config.fadeInStep.desc", 0, 100,
                                () -> this.draft.fadeInStep,
                                value -> this.edit(() -> this.draft.fadeInStep = value))
                        .enabledWhen(() -> this.draft.fadable))
                .section("narutoloading.config.section.sources", section -> section
                        .textField("narutoloading.config.sourcesDir", "narutoloading.config.sourcesDir.desc", this.draft.sourcesDir,
                                value -> this.edit(() -> this.draft.sourcesDir = value),
                                field -> field.onChange(value -> this.edit(() -> this.draft.sourcesDir = value)))
                        .dropdown("narutoloading.config.mode", "narutoloading.config.mode.desc", modes(), this.draft.selectMode,
                                value -> this.edit(() -> this.draft.selectMode = value))
                        .textField("narutoloading.config.fixedName", "narutoloading.config.fixedName.desc", this.draft.fixedName,
                                value -> this.edit(() -> this.draft.fixedName = value),
                                field -> field.onChange(value -> this.edit(() -> this.draft.fixedName = value)))
                        .enabledWhen(() -> this.draft.selectMode == MediaRepository.Mode.FIXED))
                .section("narutoloading.config.section.debug", section -> section
                        .toggle("narutoloading.config.debug", "narutoloading.config.debug.desc",
                                () -> this.draft.debugTimeCosts,
                                value -> this.edit(() -> this.draft.debugTimeCosts = value))
                        .number("narutoloading.config.debugInterval", "narutoloading.config.debugInterval.desc", 1, 600,
                                () -> this.draft.debugTimeCostsInterval,
                                value -> this.edit(() -> this.draft.debugTimeCostsInterval = value))
                        .enabledWhen(() -> this.draft.debugTimeCosts));
    }

    @Contract(" -> new")
    private static @NotNull @Unmodifiable List<MuiOption<MediaRepository.Mode>> modes() {
        return List.of(MuiOption.of(MediaRepository.Mode.RANDOM, I18n.get("narutoloading.config.mode.random")), MuiOption.of(MediaRepository.Mode.SEQUENTIAL, I18n.get("narutoloading.config.mode.sequential")), MuiOption.of(MediaRepository.Mode.FIXED, I18n.get("narutoloading.config.mode.fixed")));
    }

    private @NotNull View buildActionBar(@NotNull Context context) {
        MuiTheme theme = MuiTheme.modern(context);
        LinearLayout bar = MuiControls.row(context);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(new View(context), new LinearLayout.LayoutParams(0, 1, 1.0F));

        this.statusView = MuiControls.text(context, theme, "", 12, theme.onSurfaceVariant());
        LinearLayout.LayoutParams statusParams = MuiControls.wrap();
        statusParams.rightMargin = MuiControls.dp(context, 8);
        bar.addView(this.statusView, statusParams);

        this.revertButton = MuiControls.button(context, theme, "narutoloading.action.revert", true, view -> this.revert(), MuiShimmerable.ALWAYS);
        this.addButton(context, bar, this.revertButton);

        this.applyButton = MuiControls.button(context, theme, "narutoloading.action.apply", true, view -> this.apply(false), MuiShimmerable.ALWAYS);
        this.addButton(context, bar, this.applyButton);

        this.doneButton = MuiControls.button(context, theme, "narutoloading.action.done", false, view -> this.finish(), MuiShimmerable.ALWAYS);
        this.addButton(context, bar, this.doneButton);

        this.refreshActions();
        return bar;
    }

    private void addButton(@NotNull Context context, @NotNull LinearLayout bar, @NotNull Button button) {
        LinearLayout.LayoutParams params = MuiControls.wrap();
        params.leftMargin = MuiControls.dp(context, 8);
        params.gravity = Gravity.CENTER_VERTICAL;
        bar.addView(button, params);
    }

    private void edit(@NotNull Runnable change) {
        change.run();
        this.refreshActions();
    }

    private void refreshActions() {
        boolean dirty = this.draft.dirty();

        if (this.applyButton != null) this.applyButton.setEnabled(!this.applying && dirty);
        if (this.revertButton != null) this.revertButton.setEnabled(!this.applying && dirty);

        if (this.doneButton != null) {
            this.doneButton.setEnabled(!this.applying);
            this.doneButton.setText(I18n.get(dirty ? "narutoloading.action.doneApply" : "narutoloading.action.done"));
        }

        if (this.statusView != null) {
            this.statusView.setText(this.applying ? I18n.get("narutoloading.status.applying") : dirty ? I18n.get("narutoloading.status.pending") : "");
        }
    }

    private void finish() {
        if (this.applying) return;

        if (!this.draft.dirty()) {
            this.close();
            return;
        }

        this.apply(true);
    }

    private void apply(boolean closeAfter) {
        if (this.applying) return;

        if (!this.draft.dirty()) {
            if (closeAfter) this.close();
            return;
        }

        boolean playback = this.draft.playbackChanged();

        this.applying = true;
        this.refreshActions();

        Minecraft minecraft = Minecraft.getInstance();
        Runnable task = () -> this.applyOnRenderThread(playback, closeAfter);

        if (minecraft.isSameThread()) {
            task.run();
        } else {
            minecraft.execute(task);
        }
    }

    private void applyOnRenderThread(boolean playback, boolean closeAfter) {
        Throwable failure = null;

        try {
            this.draft.write();
            this.applyVolume();
            ClientUiControl.applyMode();

            if (playback) {
                Instance instance = Instance.get();
                instance.media.refresh();
                ClientUiControl.restart(0D);
            }
        } catch (Throwable throwable) {
            failure = throwable;
            System.err.println("[NarutoLoading] Exception applying the config: " + throwable.getMessage());
        }

        Throwable error = failure;

        MuiModApi.postToUiThread(() -> {
            this.applying = false;
            this.refreshActions();

            MuiScreenHost current = this.host;

            if (error != null) {
                if (current != null) MuiDialog.notice(current, I18n.get("narutoloading.apply.failed"), failureMessage(error), I18n.get("narutoloading.ok"), () -> {});
                return;
            }

            if (closeAfter) {
                this.close();
                return;
            }

            if (current != null) current.reloadAllPages();

            if (this.statusView != null) {
                this.statusView.setText(I18n.get("narutoloading.status.applied"));
                this.statusView.postDelayed(this::refreshActions, 1600L);
            }
        });
    }

    private void revert() {
        if (this.applying) return;

        this.draft = NarutoDraft.load();
        this.applyVolume();
        MuiShimmer.refresh();

        MuiScreenHost current = this.host;
        if (current != null) current.reloadAllPages();

        this.refreshActions();
    }

    private void applyVolume() {
        AudioProducer audio = ClientUiControl.active().audio.get();
        if (audio != null) audio.volume(this.draft.volume);
    }

    private void close() {
        MuiScreenHost current = this.host;
        if (current != null) current.close();
    }

    private boolean onCloseRequested() {
        if (this.applying) return false;
        if (!this.draft.dirty()) return true;

        MuiScreenHost current = this.host;
        if (current == null) return true;

        if (Core.isOnUiThread()) {
            this.showCloseDialog(current);
        } else {
            MuiModApi.postToUiThread(() -> this.showCloseDialog(current));
        }

        return false;
    }

    private void showCloseDialog(@NotNull MuiScreenHost host) {
        if (this.closeDialogOpen || this.host != host) return;

        this.closeDialogOpen = true;
        MuiDialog.confirm(host, I18n.get("narutoloading.unsaved.title"), I18n.get("narutoloading.unsaved.message"), I18n.get("narutoloading.unsaved.discard"),
                () -> {
                    this.closeDialogOpen = false;
                    host.close();
                }, I18n.get("narutoloading.unsaved.keep"), () -> this.closeDialogOpen = false);
    }

    private void onDestroyed() {
        this.host = null;
        FadeCenter.clearPreview();

        AudioProducer audio = ClientUiControl.active().audio.get();
        if (audio != null) audio.volume(NarutoConfig.volume());
    }

    private static @NotNull String failureMessage(@NotNull Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? I18n.get("narutoloading.apply.failedMessage") : message;
    }

    private final class ScreenShimmerConfig implements MuiShimmerConfig {
        @Override
        public boolean enabled() {
            return NarutoScreen.this.draft.shimmer;
        }

        @Override
        public int fps() {
            return NarutoScreen.this.draft.shimmerFps;
        }

        @Override
        public double speed() {
            return NarutoScreen.this.draft.shimmerSpeed;
        }

        @Override
        public double scale() {
            return NarutoScreen.this.draft.shimmerScale;
        }

        @Override
        public double strength() {
            return NarutoScreen.this.draft.shimmerStrength;
        }

        @Override
        @Contract(value = " -> new", pure = true)
        public int @NotNull [] ramp() {
            return MuiColorSchemes.NARUTO.shimmerRamp();
        }
    }
}
