package me.kall.narutoloading.client.ui;

import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.client.fade.FadeCenter;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.config.NarutoSettings;
import me.kall.narutoloading.client.ClientUiControl;
import me.kall.narutoloading.playback.produce.audio.AudioProducer;
import me.kall.narutoui.mui.color.MuiColorSchemes;
import me.kall.narutoui.mui.factory.MuiWidgetFactory;
import me.kall.narutoui.mui.layout.MuiControls;
import me.kall.narutoui.mui.screen.MuiDialog;
import me.kall.narutoui.mui.screen.MuiScreenHost;
import me.kall.narutoui.mui.screen.MuiSettingsScreen;
import me.kall.narutoui.mui.shimmer.MuiShimmer;
import me.kall.narutoui.mui.shimmer.MuiShimmerConfig;
import me.kall.narutoui.mui.shimmer.MuiShimmerProfile;
import me.kall.narutoui.mui.shimmer.MuiShimmerable;
import me.kall.narutoui.mui.theme.MuiTheme;
import me.kall.narutoui.mui.widget.MuiHintBadge;
import me.kall.narutoui.mui.widget.MuiLogo;
import me.kall.narutoloading.wall.Wall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NarutoScreen {
    private static @Nullable NarutoScreen OPEN;

    private final NarutoContext context;
    private final YtDlpPage downloads;
    private final PlaybackPage playback;
    private final ConfigPage config;
    private final WallsPage walls;
    private final MuiShimmerProfile shimmerProfile = MuiShimmerProfile.create(new ScreenShimmerConfig());

    private NarutoDraft draft = NarutoDraft.load();

    private @Nullable Wall focusWall;

    private @Nullable MuiScreenHost host;
    private @Nullable Button applyButton;
    private @Nullable Button revertButton;
    private @Nullable Button doneButton;
    private @Nullable TextView statusView;

    private boolean applying;
    private boolean closeDialogOpen;

    private NarutoScreen(@Nullable String link, @NotNull NarutoContext context) {
        this.context = context;
        this.downloads = new YtDlpPage(link, context);
        this.playback = new PlaybackPage(this, context);
        this.config = new ConfigPage(this);
        this.walls = new WallsPage(this);
        context.media().refresh();
    }

    public static void register(@NotNull FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> create(parent)));
    }

    public static @NotNull Screen create(@Nullable Screen parent) {
        return create(parent, null);
    }

    public static @NotNull Screen create(@Nullable Screen parent, @Nullable String link) {
        return new NarutoScreen(link, NarutoContext.get()).build(parent);
    }

    public static @NotNull Screen createForWall(@Nullable Screen parent, @Nullable Wall wall) {
        NarutoScreen screen = new NarutoScreen(null, NarutoContext.get());
        screen.focusWall = wall;
        return screen.build(parent);
    }

    private @NotNull Screen build(@Nullable Screen parent) {
        FadeCenter.preview(() -> this.draft.get(NarutoSettings.FADABLE), () -> this.draft.get(NarutoSettings.TICKS_BEFORE_FADE), () -> this.draft.get(NarutoSettings.FADE_OUT_STEP), () -> this.draft.get(NarutoSettings.FADE_IN_STEP));

        return MuiSettingsScreen.builder("NarutoLoading")
                .subtitle("narutoloading.screen.subtitle")
                .widgetFactory(MuiWidgetFactory.shimmer())
                .shimmerProfile(this.shimmerProfile)
                .shimmerNavigation()
                .shimmerTitles()
                .pauseGame(false)
                .railHeader((context, theme) -> MuiLogo.create(context, theme, "N"))
                .railFooter((context, theme) -> MuiHintBadge.create(context, theme, "narutoloading.ytdlp.footer"))
                .actionBar(this::buildActionBar)
                .onCreated(fragment -> {
                    this.host = fragment;
                    OPEN = this;
                    this.refreshActions();
                })
                .onDestroyed(fragment -> this.onDestroyed())
                .onCloseRequested(this::onCloseRequested)
                .initialPage(this.focusWall == null ? "narutoloading.nav.download" : "narutoloading.nav.walls")
                .page("narutoloading.nav.download", "narutoloading.ytdlp.download.title", "narutoloading.ytdlp.download.subtitle", this.downloads::build)
                .page("narutoloading.nav.playback", "narutoloading.playback.title", "narutoloading.playback.subtitle", this.playback::build)
                .page("narutoloading.nav.config", "narutoloading.config.title", "narutoloading.config.subtitle", this.config::build)
                .page("narutoloading.nav.help", "narutoloading.help.title", "narutoloading.help.subtitle", HelpPage::build)
                .page("narutoloading.nav.walls", "narutoloading.walls.title", "narutoloading.walls.subtitle", this.walls::build)
                .create(parent);
    }

    Wall focusWall() {
        Wall wall = this.focusWall;
        this.focusWall = null;
        return wall;
    }

    @Nullable MuiScreenHost host() {
        return this.host;
    }

    public static void remoteChanged() {
        NarutoScreen screen = OPEN;
        if (screen == null) return;

        MuiModApi.postToUiThread(screen::reloadPage);
    }

    @NotNull NarutoDraft draft() {
        return this.draft;
    }

    void edit(@NotNull Runnable change) {
        change.run();
        this.refreshActions();
    }

    void reloadPage() {
        MuiScreenHost current = this.host;
        if (current != null) current.reloadCurrentPage();
    }

    void notifyImport(@NotNull String message) {
        MuiModApi.postToUiThread(() -> {
            MuiScreenHost current = this.host;
            if (current != null) MuiDialog.notice(current, I18n.get("narutoloading.playback.import.title"), message, I18n.get("narutoloading.ok"), () -> {});
        });
    }

    void applyVolume() {
        AudioProducer audio = ClientUiControl.active().audio.get();
        if (audio != null) audio.volume(this.draft.get(NarutoSettings.AUDIO_VOLUME));
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
        double seekTo = playback ? ClientUiControl.positionSec() : 0D;

        try {
            this.draft.write();
            this.applyVolume();
            ClientUiControl.applyMode();

            if (playback) {
                this.context.media().refresh();
                ClientUiControl.restart(seekTo);
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
        if (OPEN == this) OPEN = null;
        this.downloads.destroy();
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
            return NarutoScreen.this.draft.get(NarutoSettings.SHIMMER);
        }

        @Override
        public int fps() {
            return NarutoScreen.this.draft.get(NarutoSettings.SHIMMER_FPS);
        }

        @Override
        public double speed() {
            return NarutoScreen.this.draft.get(NarutoSettings.SHIMMER_SPEED);
        }

        @Override
        public double scale() {
            return NarutoScreen.this.draft.get(NarutoSettings.SHIMMER_SCALE);
        }

        @Override
        public double strength() {
            return NarutoScreen.this.draft.get(NarutoSettings.SHIMMER_STRENGTH);
        }

        @Override
        @Contract(value = " -> new", pure = true)
        public int @NotNull [] ramp() {
            return MuiColorSchemes.NARUTO.shimmerRamp();
        }
    }
}
