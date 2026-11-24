package me.kall.narutoloading.client.ui;

import icyllis.modernui.core.Context;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import me.kall.narutoloading.client.ClientUiControl;
import me.kall.narutoloading.client.fade.FadeCenter;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.config.NarutoSettings;
import me.kall.narutoloading.config.Setting;
import me.kall.narutoloading.core.NarutoContext;
import me.kall.narutoloading.core.util.GamePaths;
import me.kall.narutoloading.playback.produce.audio.AudioProducer;
import me.kall.narutoloading.wall.Wall;
import me.kall.narutoui.mui.background.MuiBackground;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

public final class NarutoScreen {
    private static final long STATUS_MILLIS = 4500L;
    private static final long SAVE_DELAY_MILLIS = 400L;
    private static final long PLAYBACK_DELAY_MILLIS = 700L;

    private static final Set<Setting<?>> PLAYBACK_SETTINGS = Set.of(NarutoSettings.MAX_VIDEO_WIDTH, NarutoSettings.MAX_VIDEO_HEIGHT);

    private static @Nullable NarutoScreen OPEN;

    private final NarutoContext context;
    private final MediaPage mediaPage;
    private final LoadingPage loadingPage;
    private final WallsPage wallsPage;
    private final GeneralPage generalPage;
    private final @Nullable WallEditorPage wallEditorPage;
    private final NarutoSettingsState state = NarutoSettingsState.load();
    private final MuiShimmerProfile shimmerProfile = MuiShimmerProfile.create(new ScreenShimmerConfig());

    private @Nullable MuiScreenHost host;
    private @Nullable LinearLayout statusBar;
    private @Nullable TextView statusView;
    private @Nullable Button undoButton;

    private final Set<Setting<?>> pendingWrites = new LinkedHashSet<>();
    private @Nullable Setting<?> undoSetting;
    private @Nullable Runnable undoAction;

    private @Nullable String statusText;
    private long statusShownAt;

    private int saveGeneration;
    private int playbackGeneration;
    private int statusGeneration;

    private boolean downloadFocus;
    private boolean bulk;
    private boolean destroyed;

    private NarutoScreen(@Nullable String link, @Nullable Wall wall, @NotNull NarutoContext context) {
        this.context = context;
        this.downloadFocus = link != null && !link.isBlank();
        this.mediaPage = new MediaPage(this, context, link);
        this.loadingPage = new LoadingPage(this);
        this.wallsPage = new WallsPage(this, context);
        this.generalPage = new GeneralPage(this, context);
        this.wallEditorPage = wall == null ? null : new WallEditorPage(this, wall);
        context.media().refresh();
    }

    public static void register(@NotNull ModContainer context) {
        context.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> create(parent));
    }

    public static @NotNull Screen create(@Nullable Screen parent) {
        return create(parent, null);
    }

    public static @NotNull Screen create(@Nullable Screen parent, @Nullable String link) {
        return new NarutoScreen(link, null, NarutoContext.get()).build(parent);
    }

    public static @NotNull Screen createForWall(@Nullable Screen parent, @Nullable Wall wall) {
        if (wall == null) return create(parent);
        return new NarutoScreen(null, wall, NarutoContext.get()).build(parent);
    }

    private @NotNull Screen build(@Nullable Screen parent) {
        FadeCenter.preview(() -> this.state.get(NarutoSettings.FADABLE), () -> this.state.get(NarutoSettings.TICKS_BEFORE_FADE), () -> this.state.get(NarutoSettings.FADE_OUT_STEP), () -> this.state.get(NarutoSettings.FADE_IN_STEP));

        MuiSettingsScreen.Builder builder = MuiSettingsScreen.builder("NarutoLoading")
                .subtitle("narutoloading.screen.subtitle")
                .widgetFactory(MuiWidgetFactory.shimmer())
                .shimmerProfile(this.shimmerProfile)
                .shimmerNavigation()
                .shimmerTitles()
                .pauseGame(false)
                .background(MuiBackground.transparent())
                .themeProvider(context -> NarutoTheme.resolve(this.state.get(NarutoSettings.THEME), context))
                .railHeader((context, theme) -> MuiLogo.create(context, theme, "N"))
                .railFooter((context, theme) -> MuiHintBadge.create(context, theme, "narutoloading.screen.footer"))
                .actionBar(this::buildStatusBar)
                .onCreated(fragment -> {
                    this.host = fragment;
                    OPEN = this;
                    this.ensureMediaScanned();
                })
                .onDestroyed(fragment -> this.onDestroyed());

        WallEditorPage editor = this.wallEditorPage;

        if (editor != null) {
            builder.singlePage()
                    .page("narutoloading.nav.wall", editor.title(), editor.subtitle(), editor::build);
        } else {
            builder.initialPage("narutoloading.nav.media")
                    .page("narutoloading.nav.media", "narutoloading.media.title", "narutoloading.media.subtitle", this.mediaPage::build)
                    .page("narutoloading.nav.loading", "narutoloading.loading.title", "narutoloading.loading.subtitle", this.loadingPage::build)
                    .page("narutoloading.nav.walls", "narutoloading.walls.title", "narutoloading.walls.subtitle", this.wallsPage::build)
                    .page("narutoloading.nav.general", "narutoloading.general.title", "narutoloading.general.subtitle", this.generalPage::build);
        }

        return builder.create(parent);
    }

    @Nullable MuiScreenHost host() {
        return this.host;
    }

    @NotNull NarutoSettingsState settings() {
        return this.state;
    }

    public static void remoteChanged() {
        NarutoScreen screen = OPEN;
        if (screen == null) return;

        MuiModApi.postToUiThread(screen::reloadPage);
    }

    void reloadPage() {
        MuiScreenHost current = this.host;
        if (current != null) current.reloadCurrentPage();
    }

    void notifyImport(@NotNull String message) {
        MuiModApi.postToUiThread(() -> {
            MuiScreenHost current = this.host;
            if (current != null) MuiDialog.notice(current, I18n.get("narutoloading.media.import.title"), message, I18n.get("narutoloading.ok"), () -> {});
        });
    }

    boolean consumeDownloadFocus() {
        boolean focus = this.downloadFocus;
        this.downloadFocus = false;
        return focus;
    }

    void applyVolume() {
        AudioProducer audio = ClientUiControl.active().audio.get();
        if (audio != null) audio.volume(this.state.get(NarutoSettings.AUDIO_VOLUME));
    }

    @NotNull Path sourcesFolder() {
        try {
            return GamePaths.resolve(Path.of(this.state.get(NarutoSettings.SOURCES_DIR)));
        } catch (RuntimeException exception) {
            return GamePaths.gameDir();
        }
    }

    private void ensureMediaScanned() {
        if (!this.context.media().all().isEmpty()) return;

        this.context.media().refresh(() -> MuiModApi.postToUiThread(() -> {
            if (this.destroyed) return;
            this.reloadPage();
        }));
    }

    void reloadSources() {
        Minecraft minecraft = Minecraft.getInstance();
        Runnable task = () -> this.context.media().refresh(() -> minecraft.execute(() -> {
            ClientUiControl.restart(0D);
            MuiModApi.postToUiThread(() -> {
                if (this.destroyed) return;
                this.reloadPage();
            });
        }));

        if (minecraft.isSameThread()) {
            task.run();
        } else {
            minecraft.execute(task);
        }
    }

    <T> void set(@NotNull Setting<T> setting, T value) {
        this.set(setting, value, true);
    }

    private <T> void set(@NotNull Setting<T> setting, T value, boolean recordUndo) {
        T previous = this.state.get(setting);
        if (Objects.equals(previous, value)) return;

        if (recordUndo && !this.bulk && this.undoSetting != setting) {
            this.undoSetting = setting;
            this.undoAction = () -> this.set(setting, previous, false);
        }

        this.state.set(setting, value);
        this.changed(setting);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void resetDefaults() {
        this.undoAction = null;
        this.undoSetting = null;
        this.bulk = true;

        try {
            for (Setting setting : NarutoSettings.ALL) this.set(setting, setting.defaultValue(), false);
        } finally {
            this.bulk = false;
        }

        this.flush();
        this.applyVolume();
        this.context.reloadFFmpeg();
        this.context.tools().reloadYtDlp();
        MuiShimmer.refresh();

        Minecraft minecraft = Minecraft.getInstance();
        Runnable apply = () -> {
            this.context.media().refresh();
            ClientUiControl.restart(0D);
        };

        if (minecraft.isSameThread()) {
            apply.run();
        } else {
            minecraft.execute(apply);
        }

        MuiScreenHost current = this.host;
        if (current != null) current.onThemeChanged();

        this.showStatus(I18n.get("narutoloading.status.reset"), false);
    }

    void status(@NotNull CharSequence text) {
        if (this.destroyed) return;
        this.showStatus(text, false);
    }

    private <T> void changed(@NotNull Setting<T> setting) {
        this.pendingWrites.add(setting);
        this.scheduleSave();

        if (setting == NarutoSettings.AUDIO_VOLUME) {
            this.applyVolume();
        } else if (isShimmer(setting)) {
            MuiShimmer.refresh();
        } else if (setting == NarutoSettings.THEME && !this.bulk) {
            this.flush();
            MuiScreenHost current = this.host;
            if (current != null) current.onThemeChanged();
        }

        if (PLAYBACK_SETTINGS.contains(setting)) this.schedulePlaybackReload();
        if (!this.bulk) this.showStatus(I18n.get("narutoloading.status.saved"), true);
    }

    private static boolean isShimmer(@NotNull Setting<?> setting) {
        return setting == NarutoSettings.SHIMMER || setting == NarutoSettings.SHIMMER_SPEED || setting == NarutoSettings.SHIMMER_SCALE || setting == NarutoSettings.SHIMMER_STRENGTH || setting == NarutoSettings.SHIMMER_FPS;
    }

    private void scheduleSave() {
        int generation = ++this.saveGeneration;

        TextView view = this.statusView;
        if (view == null) {
            this.flush();
            return;
        }

        view.postDelayed(() -> {
            if (generation == this.saveGeneration) this.flush();
        }, SAVE_DELAY_MILLIS);
    }

    private void flush() {
        if (this.pendingWrites.isEmpty()) return;

        Map<Setting<?>, Object> changed = new LinkedHashMap<>();
        for (Setting<?> setting : this.pendingWrites) changed.put(setting, this.state.get(setting));
        this.pendingWrites.clear();

        try {
            NarutoConfig.writeAll(changed);
            NarutoConfig.save();
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading] Exception saving the config: " + throwable.getMessage());
            if (!this.destroyed) this.showStatus(I18n.get("narutoloading.status.saveFailed"), false);
        }
    }

    private void schedulePlaybackReload() {
        int generation = ++this.playbackGeneration;

        TextView view = this.statusView;
        if (view == null) return;

        view.postDelayed(() -> {
            if (generation != this.playbackGeneration) return;

            Minecraft.getInstance().execute(() -> {
                double seekTo = ClientUiControl.positionSec();
                this.context.media().refresh();
                ClientUiControl.restart(seekTo);
            });
        }, PLAYBACK_DELAY_MILLIS);
    }

    private void undo() {
        Runnable action = this.undoAction;
        if (action == null) return;

        this.undoAction = null;
        this.undoSetting = null;
        action.run();

        MuiScreenHost current = this.host;
        if (current != null) current.reloadCurrentPage();

        this.showStatus(I18n.get("narutoloading.status.undone"), false);
    }

    private @NotNull View buildStatusBar(@NotNull Context context) {
        MuiTheme theme = NarutoTheme.resolve(this.state.get(NarutoSettings.THEME), context);
        LinearLayout bar = MuiControls.row(context);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        this.statusBar = bar;

        bar.addView(new View(context), new LinearLayout.LayoutParams(0, 1, 1.0F));

        this.statusView = MuiControls.text(context, theme, "", 12, theme.onSurfaceVariant());
        LinearLayout.LayoutParams statusParams = MuiControls.wrap();
        statusParams.rightMargin = MuiControls.dp(context, 8);
        bar.addView(this.statusView, statusParams);

        this.undoButton = MuiControls.button(context, theme, "narutoloading.action.undo", true, view -> this.undo(), MuiShimmerable.ALWAYS);
        LinearLayout.LayoutParams undoParams = MuiControls.wrap();
        undoParams.leftMargin = MuiControls.dp(context, 8);
        undoParams.gravity = Gravity.CENTER_VERTICAL;
        bar.addView(this.undoButton, undoParams);

        this.restoreStatus();
        return bar;
    }

    private void showStatus(@NotNull CharSequence text, boolean undoable) {
        this.statusText = text.toString();
        this.statusShownAt = System.currentTimeMillis();
        this.paintStatus(undoable, STATUS_MILLIS);
    }

    private void restoreStatus() {
        String text = this.statusText;
        if (text == null) {
            if (this.statusBar != null) this.statusBar.setVisibility(View.GONE);
            return;
        }

        long elapsed = System.currentTimeMillis() - this.statusShownAt;
        if (elapsed >= STATUS_MILLIS) {
            if (this.statusBar != null) this.statusBar.setVisibility(View.GONE);
            return;
        }

        this.paintStatus(true, STATUS_MILLIS - elapsed);
    }

    private void paintStatus(boolean undoable, long millis) {
        LinearLayout bar = this.statusBar;
        TextView view = this.statusView;
        if (bar == null || view == null) return;

        if (this.statusText != null) view.setText(this.statusText);
        bar.setVisibility(View.VISIBLE);
        if (this.undoButton != null) {
            this.undoButton.setVisibility(undoable && this.undoAction != null ? View.VISIBLE : View.GONE);
        }

        int generation = ++this.statusGeneration;
        view.postDelayed(() -> {
            if (generation == this.statusGeneration) bar.setVisibility(View.GONE);
        }, Math.max(0L, millis));
    }

    private void onDestroyed() {
        this.destroyed = true;
        this.flush();
        this.mediaPage.destroy();
        this.wallsPage.destroy();
        WallEditorPage editor = this.wallEditorPage;
        if (editor != null) editor.destroy();

        this.host = null;
        if (OPEN == this) OPEN = null;
        FadeCenter.clearPreview();

        AudioProducer audio = ClientUiControl.active().audio.get();
        if (audio != null) audio.volume(NarutoConfig.volume());
    }

    private final class ScreenShimmerConfig implements MuiShimmerConfig {
        @Override
        public boolean enabled() {
            return NarutoScreen.this.state.get(NarutoSettings.SHIMMER);
        }

        @Override
        public int fps() {
            return NarutoScreen.this.state.get(NarutoSettings.SHIMMER_FPS);
        }

        @Override
        public double speed() {
            return NarutoScreen.this.state.get(NarutoSettings.SHIMMER_SPEED);
        }

        @Override
        public double scale() {
            return NarutoScreen.this.state.get(NarutoSettings.SHIMMER_SCALE);
        }

        @Override
        public double strength() {
            return NarutoScreen.this.state.get(NarutoSettings.SHIMMER_STRENGTH);
        }

        @Override
        @Contract(value = " -> new", pure = true)
        public int @NotNull [] ramp() {
            return NarutoTheme.ramp(NarutoScreen.this.state.get(NarutoSettings.THEME));
        }
    }
}
