package me.kall.narutoloading.client.ui;

import icyllis.modernui.core.Context;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import me.kall.narutoloading.media.DownloadService;
import me.kall.narutoloading.media.DownloadState;
import me.kall.narutoloading.core.GamePaths;
import me.kall.narutoloading.media.MediaNaming;
import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.core.ToolRegistry;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.client.ClientUiControl;
import me.kall.narutoui.mui.field.MuiTextField;
import me.kall.narutoui.mui.layout.MuiControls;
import me.kall.narutoui.mui.layout.MuiLayout;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import me.kall.narutoui.mui.screen.MuiSettingsSection;
import me.kall.narutoui.mui.theme.MuiTheme;
import me.kall.narutoui.mui.util.MuiFilePicker;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

final class YtDlpPage implements SettingsPageBuilder {
    private final ToolRegistry tools;
    private final DownloadService downloads;
    private final MediaRepository media;

    private final Runnable stateListener = this::onStateChanged;
    private final AtomicBoolean renderPending = new AtomicBoolean();

    private boolean subscribed;

    private @Nullable String executable;

    private @Nullable MuiTextField linkField;
    private @Nullable MuiTextField nameField;
    private @Nullable MuiSettingsSection statusSection;
    private @Nullable TextView nameWarning;
    private @Nullable TextView statusView;
    private @Nullable TextView logView;
    private @Nullable MuiTheme theme;

    private volatile String link;
    private volatile String fileName = "";

    YtDlpPage(@Nullable String link, @NotNull NarutoContext context) {
        this.tools = context.tools();
        this.downloads = context.downloads();
        this.media = context.media();
        this.executable = this.tools.ytDlp.ytdlp.get();
        this.link = link == null ? "" : link.trim();
    }

    @Override
    public void build(@NotNull MuiSettingsPage page) {
        this.subscribe();

        page.shimmerAll();
        this.theme = page.theme();

        page
                .section("narutoloading.ytdlp.section.status", section -> {
                    this.statusSection = section;
                    this.fillStatus(section);
                })
                .section("narutoloading.ytdlp.section.link", section -> {
                    Context context = section.page().context();
                    MuiTheme theme = section.page().theme();

                    section
                            .textField("narutoloading.ytdlp.link", "narutoloading.ytdlp.link.desc", this.link, value -> this.link = value, field -> {
                                this.linkField = field;
                                field.hint("narutoloading.ytdlp.link.hint").onChange(value -> this.link = value).softValidator(YtDlpPage::looksLikeUrl);
                            })
                            .textField("narutoloading.ytdlp.name", "narutoloading.ytdlp.name.desc", this.fileName, value -> this.fileName = value, field -> {
                                this.nameField = field;
                                field.hint("narutoloading.ytdlp.name.hint").onChange(value -> {
                                    this.fileName = value;
                                    this.refreshNameWarning();
                                }).softValidator(YtDlpPage::isValidName);
                            });

                    this.nameWarning = MuiControls.text(context, theme, "", 10, theme.primary());
                    this.nameWarning.setPadding(MuiControls.dp(context, 2), MuiControls.dp(context, 2), MuiControls.dp(context, 2), 0);
                    this.nameWarning.setVisibility(View.GONE);

                    LinearLayout.LayoutParams warningParams = MuiControls.fill();
                    warningParams.topMargin = MuiControls.dp(context, 2);
                    section.card().addView(this.nameWarning, warningParams);

                    section
                            .value("narutoloading.ytdlp.target", this.outputDirectory().toString())
                            .button("narutoloading.ytdlp.action.video", false, () -> this.start(true)).enabledWhen(this::canDownload)
                            .button("narutoloading.ytdlp.action.audio", true, () -> this.start(false)).enabledWhen(this::canDownload)
                            .note("narutoloading.ytdlp.action.note", "narutoloading.ytdlp.action.note.desc");

                    this.refreshNameWarning();
                })
                .section("narutoloading.ytdlp.section.progress", section -> section
                        .value("narutoloading.ytdlp.progress.percent", value -> value < 0 ? "—" : value + "%", () -> this.downloads.state().percent())
                        .customLayout(row -> {
                            this.statusView = MuiControls.text(row.getContext(), row.theme(), this.currentStatus(), 13, this.currentStatusColor(row.theme()));
                            row.add(this.statusView, MuiControls.fill());
                        })
                        .customLayout(row -> {
                            this.logView = MuiControls.text(row.getContext(), row.theme(), this.currentLog(), 11, row.theme().onSurfaceVariant());
                            row.add(this.logView, MuiControls.fill());
                        })
                        .button("narutoloading.ytdlp.action.openFolder", true, this::openFolder)
                );
    }

    void destroy() {
        if (!this.subscribed) return;

        this.subscribed = false;
        this.downloads.state().removeListener(this.stateListener);
    }

    private void subscribe() {
        if (this.subscribed) return;

        this.subscribed = true;
        this.downloads.state().addListener(this.stateListener);
    }

    private void fillStatus(@NotNull MuiSettingsSection section) {
        boolean available = this.executable != null;

        section.value("narutoloading.ytdlp.status.state", I18n.get(available ? "narutoloading.ytdlp.status.found" : "narutoloading.ytdlp.status.missing"))
                .value("narutoloading.ytdlp.status.path", available ? this.executable : I18n.get("narutoloading.ytdlp.status.path.none"));

        if (!available) section.note("narutoloading.ytdlp.status.hint", "narutoloading.ytdlp.status.hint.desc");

        section.customLayout(row -> {
            Context context = row.getContext();
            MuiTheme theme = row.theme();

            Button choose = MuiControls.button(context, theme, "narutoloading.ytdlp.action.choose", true, view -> this.chooseExecutable());
            Button redetect = MuiControls.button(context, theme, "narutoloading.ytdlp.action.redetect", true, view -> this.redetect());

            row.add(MuiLayout.row(context, theme).padding(0).spacing(8).addWeighted(choose, 1.0F).addWeighted(redetect, 1.0F), MuiControls.fill());
        });
    }

    private void chooseExecutable() {
        MuiFilePicker.openFile(I18n.get("narutoloading.ytdlp.action.choose"), "yt-dlp", this::installExecutable, ToolRegistry.YTDLP_FILE_FILTER);
    }

    private void installExecutable(@NotNull Path source) {
        if (!Files.isRegularFile(source)) {
            this.publishStatus(I18n.get("narutoloading.ytdlp.status.install.invalid", source.getFileName().toString()), true);
            return;
        }

        String installed = this.tools.installYtDlp(source);

        if (installed == null) {
            this.executable = null;
            this.refreshStatus();
            this.publishStatus(I18n.get("narutoloading.ytdlp.status.install.failed"), true);
            return;
        }

        Path parent = source.getParent();
        if (parent != null) {
            NarutoConfig.set(NarutoConfig.YTDLP_DIR, parent.toString());
            NarutoConfig.save();
            installed = this.tools.reloadYtDlp();
        }

        this.executable = installed;
        this.refreshStatus();

        this.publishStatus(I18n.get("narutoloading.ytdlp.status.install.done", source.getFileName().toString()), false);
    }

    private void redetect() {
        this.executable = this.tools.reloadYtDlp();
        this.refreshStatus();

        this.publishStatus(I18n.get(this.executable != null ? "narutoloading.ytdlp.status.redetect.found" : "narutoloading.ytdlp.status.redetect.missing"), this.executable == null);
    }

    private void refreshStatus() {
        MuiSettingsSection section = this.statusSection;
        if (section != null) section.rebuild(this::fillStatus);
    }

    private void start(boolean video) {
        if (this.executable == null) return;

        String url = this.currentLink();
        String name = this.currentName();
        if (url.isEmpty() || !isValidName(name)) return;

        DownloadState state = this.downloads.state();
        if (!state.begin()) return;

        state.status(I18n.get(video ? "narutoloading.ytdlp.status.start.video" : "narutoloading.ytdlp.status.start.audio"), false);

        Path outputDirectory = this.outputDirectory();
        CompletableFuture<@Nullable String> future = video ? this.downloads.downloadVideo(url, outputDirectory, name, this::onOutputLine) : this.downloads.downloadAudio(url, outputDirectory, name, this::onOutputLine);

        future.whenComplete((result, throwable) -> {
            state.end();

            if (throwable != null) {
                System.err.println("[NarutoLoading] Exception downloading " + url + ": " + throwable.getMessage());
                state.status(I18n.get("narutoloading.ytdlp.status.failed", rootMessage(throwable)), true);
                return;
            }

            if (result == null) {
                state.status(I18n.get("narutoloading.ytdlp.status.failed", I18n.get("narutoloading.ytdlp.status.failed.detail")), true);
                return;
            }

            state.done(I18n.get("narutoloading.ytdlp.status.done", Path.of(result).getFileName().toString()));

            String mediaName = MediaNaming.mediaName(result);
            this.media.refresh(() -> this.play(mediaName));
            this.postNameWarning();
        });
    }

    private void play(@NotNull String name) {
        Minecraft.getInstance().execute(() -> {
            if (this.media.select(name)) {
                ClientUiControl.restart(0D);
                return;
            }

            this.publishStatus(I18n.get("narutoloading.ytdlp.status.unplayable", name), true);
        });
    }

    private void onOutputLine(@NotNull String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return;

        this.downloads.state().line(trimmed);
    }

    private void onStateChanged() {
        if (!this.renderPending.compareAndSet(false, true)) return;

        TextView view = this.statusView != null ? this.statusView : this.logView;

        if (view == null) {
            this.renderPending.set(false);
            return;
        }

        view.post(() -> {
            this.renderPending.set(false);
            this.renderState();
        });
    }

    private void renderState() {
        TextView statusView = this.statusView;

        if (statusView != null) {
            MuiTheme theme = this.theme;
            statusView.setText(this.currentStatus());
            if (theme != null) statusView.setTextColor(this.currentStatusColor(theme));
        }

        TextView logView = this.logView;
        if (logView != null) logView.setText(this.currentLog());
    }

    private void publishStatus(@NotNull String text, boolean failed) {
        this.downloads.state().status(text, failed);
    }

    private @NotNull String currentStatus() {
        DownloadState state = this.downloads.state();
        if (!state.status().isEmpty()) return state.status();
        return I18n.get(state.downloading() ? "narutoloading.ytdlp.status.working" : "narutoloading.ytdlp.status.idle");
    }

    private int currentStatusColor(@NotNull MuiTheme theme) {
        return this.downloads.state().statusFailed() ? theme.error() : theme.onSurface();
    }

    private @NotNull String currentLog() {
        String log = this.downloads.state().logText();
        return log.isEmpty() ? I18n.get("narutoloading.ytdlp.progress.log.empty") : log;
    }

    private boolean canDownload() {
        return this.executable != null && !this.downloads.state().downloading() && !this.currentLink().isEmpty() && isValidName(this.currentName());
    }

    private @NotNull String currentLink() {
        MuiTextField field = this.linkField;
        String text = field == null ? this.link : field.text();
        return text == null ? "" : text.trim();
    }

    private @NotNull String currentName() {
        MuiTextField field = this.nameField;
        String text = field == null ? this.fileName : field.text();
        return text == null ? "" : text.trim();
    }

    private void refreshNameWarning() {
        TextView view = this.nameWarning;
        if (view == null) return;

        String name = this.currentName();
        String summary = isValidName(name) ? this.collisionSummary(name) : null;

        view.setText(summary == null ? "" : summary);
        view.setVisibility(summary == null ? View.GONE : View.VISIBLE);
    }

    private void postNameWarning() {
        TextView view = this.nameWarning;
        if (view != null) view.post(this::refreshNameWarning);
    }

    private @Nullable String collisionSummary(@NotNull String name) {
        Path outputDirectory = this.outputDirectory();
        if (!Files.isDirectory(outputDirectory)) return null;

        List<String> matches = new ArrayList<>();
        try (Stream<Path> stream = Files.list(outputDirectory)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(MediaNaming::isMediaFile)
                    .filter(fileName -> MediaNaming.wouldOverwrite(name, fileName))
                    .sorted().forEach(matches::add);
        } catch (IOException | UncheckedIOException exception) {
            return null;
        }

        return matches.isEmpty() ? null : I18n.get("narutoloading.ytdlp.name.conflict", String.join(", ", matches));
    }

    private void openFolder() {
        Path outputDirectory = this.outputDirectory();
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Exception creating " + outputDirectory + ": " + exception.getMessage());
        }

        try {
            Util.getPlatform().openFile(outputDirectory.toFile());
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading] Exception opening " + outputDirectory + ": " + throwable.getMessage());
        }
    }

    private @NotNull Path outputDirectory() {
        return GamePaths.resolve(NarutoConfig.sourcesDir());
    }

    private static boolean looksLikeUrl(@NotNull String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }

    private static boolean isValidName(@NotNull String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.equals(".") || trimmed.equals("..")) return false;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < ' ' || "/\\:*?\"<>|".indexOf(c) >= 0) return false;
        }

        return true;
    }

    private static @NotNull String rootMessage(@NotNull Throwable throwable) {
        Throwable cause = throwable.getCause();
        String message = (cause == null ? throwable : cause).getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
