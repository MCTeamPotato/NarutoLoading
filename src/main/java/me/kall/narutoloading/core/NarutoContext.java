package me.kall.narutoloading.core;

import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.core.tool.ToolRegistry;
import me.kall.narutoloading.core.util.Workers;
import me.kall.narutoloading.media.download.DownloadService;
import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.media.probe.ProbeService;
import me.kall.narutoloading.playback.PlaybackModes;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class NarutoContext {
    private static final NarutoContext INSTANCE = new NarutoContext();

    private final Workers workers;
    private final ToolRegistry tools;
    private final ProbeService probe;
    private final MediaRepository media;
    private final DownloadService downloads;
    private final PlaybackModes modes;

    private NarutoContext() {
        this.workers = new Workers();
        this.tools = new ToolRegistry();
        this.probe = new ProbeService(this.tools.ffmpeg, this.workers.probe());
        this.media = new MediaRepository(NarutoConfig::sourcesDir, NarutoConfig::selectMode, NarutoConfig::fixedName, this.probe::readVideo, this.probe::readAudio, this.workers.download());
        this.downloads = new DownloadService(this.tools.ytDlp, this.workers.download());
        this.modes = new PlaybackModes(this.media, this.tools.ffmpeg);
    }

    public static @NotNull NarutoContext get() {
        return INSTANCE;
    }

    public @NotNull Workers workers() {
        return this.workers;
    }

    public @NotNull ToolRegistry tools() {
        return this.tools;
    }

    public @NotNull ProbeService probe() {
        return this.probe;
    }

    public @NotNull MediaRepository media() {
        return this.media;
    }

    public @NotNull DownloadService downloads() {
        return this.downloads;
    }

    public @NotNull PlaybackModes modes() {
        return this.modes;
    }

    public void reloadFFmpeg() {
        this.tools.reloadFFmpeg();
        this.media.invalidate();
    }

    public void useFfmpegDirectory(@NotNull Path directory) {
        NarutoConfig.set(NarutoConfig.FFMPEG_DIR, directory.toString());
        NarutoConfig.save();
        this.reloadFFmpeg();
    }
}
