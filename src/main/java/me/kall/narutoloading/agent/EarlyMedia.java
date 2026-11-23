package me.kall.narutoloading.agent;

import me.kall.narutoloading.core.util.GamePaths;
import me.kall.narutoloading.core.tool.Locator;
import me.kall.narutoloading.media.MediaRepository;
import me.kall.narutoloading.media.ffmpeg.FFmpeg;
import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.media.MediaSource;
import me.kall.narutoloading.config.NarutoConfig;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

final class EarlyMedia {
    final String ffmpeg;
    final VideoArgs video;
    final AudioArgs audio;
    final float volume;

    private EarlyMedia(String ffmpeg, VideoArgs video, AudioArgs audio, float volume) {
        this.ffmpeg = ffmpeg;
        this.video = video;
        this.audio = audio;
        this.volume = volume;
    }

    static @Nullable EarlyMedia resolve() {
        Path gameDir = GamePaths.gameDir();
        String configuredDir = NarutoConfig.ffmpegDir().toString();

        String ffmpeg = Locator.locate("ffmpeg", gameDir, configuredDir);
        String ffprobe = Locator.locate("ffprobe", gameDir, configuredDir);
        if (ffmpeg == null || ffprobe == null) return null;

        MediaSource source = MediaRepository.pickSource(gameDir.resolve(NarutoConfig.sourcesDir()), NarutoConfig.selectMode(), NarutoConfig.fixedName());
        if (source == null) return null;

        FFmpeg tools = new FFmpeg();
        tools.ffmpeg.set(ffmpeg);
        tools.ffprobe.set(ffprobe);

        VideoArgs video = tools.deriveVideo(source.video());
        if (video == null) return null;

        AudioArgs audio = tools.deriveAudio(source.audio() != null ? source.audio() : source.video());

        return new EarlyMedia(ffmpeg, video, audio, NarutoConfig.volume());
    }
}
