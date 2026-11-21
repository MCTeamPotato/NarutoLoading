package me.kall.narutoloading.client.ui;

import me.kall.narutoloading.app.MediaRepository;
import me.kall.narutoloading.config.NarutoConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

final class NarutoDraft {
    int maxVideoWidth;
    int maxVideoHeight;
    float volume;
    String sourcesDir;
    double bufferSeconds;
    MediaRepository.Mode selectMode;
    String fixedName;
    boolean muteMusic;
    boolean debugTimeCosts;
    int debugTimeCostsInterval;
    boolean shimmer;
    double shimmerSpeed;
    double shimmerScale;
    double shimmerStrength;
    int shimmerFps;
    boolean fadable;
    int ticksBeforeFade;

    private NarutoDraft() {
        this.maxVideoWidth = NarutoConfig.limitWidth();
        this.maxVideoHeight = NarutoConfig.limitHeight();
        this.volume = NarutoConfig.volume();
        this.sourcesDir = NarutoConfig.sourcesDir().toString();
        this.bufferSeconds = NarutoConfig.bufferSeconds();
        this.selectMode = NarutoConfig.selectMode();
        this.fixedName = NarutoConfig.fixedName();
        this.muteMusic = NarutoConfig.muteMusic();
        this.debugTimeCosts = NarutoConfig.debugTimeCosts();
        this.debugTimeCostsInterval = NarutoConfig.debugTimeCostsInterval();
        this.shimmer = NarutoConfig.shimmer();
        this.shimmerSpeed = NarutoConfig.shimmerSpeed();
        this.shimmerScale = NarutoConfig.shimmerScale();
        this.shimmerStrength = NarutoConfig.shimmerStrength();
        this.shimmerFps = NarutoConfig.shimmerFps();
        this.fadable = NarutoConfig.fadable();
        this.ticksBeforeFade = NarutoConfig.ticksBeforeFade();
    }

    static @NotNull NarutoDraft load() {
        return new NarutoDraft();
    }

    boolean dirty() {
        return this.maxVideoWidth != NarutoConfig.limitWidth()
                || this.maxVideoHeight != NarutoConfig.limitHeight()
                || Float.compare(this.volume, NarutoConfig.volume()) != 0
                || !this.sourcesDir.equals(NarutoConfig.sourcesDir().toString())
                || Double.compare(this.bufferSeconds, NarutoConfig.bufferSeconds()) != 0
                || this.selectMode != NarutoConfig.selectMode()
                || !this.fixedName.equals(NarutoConfig.fixedName())
                || this.muteMusic != NarutoConfig.muteMusic()
                || this.debugTimeCosts != NarutoConfig.debugTimeCosts()
                || this.debugTimeCostsInterval != NarutoConfig.debugTimeCostsInterval()
                || this.shimmer != NarutoConfig.shimmer()
                || Double.compare(this.shimmerSpeed, NarutoConfig.shimmerSpeed()) != 0
                || Double.compare(this.shimmerScale, NarutoConfig.shimmerScale()) != 0
                || Double.compare(this.shimmerStrength, NarutoConfig.shimmerStrength()) != 0
                || this.shimmerFps != NarutoConfig.shimmerFps()
                || this.fadable != NarutoConfig.fadable()
                || this.ticksBeforeFade != NarutoConfig.ticksBeforeFade();
    }

    boolean playbackChanged() {
        return !this.sourcesDir.equals(NarutoConfig.sourcesDir().toString())
                || this.selectMode != NarutoConfig.selectMode()
                || !this.fixedName.equals(NarutoConfig.fixedName())
                || this.maxVideoWidth != NarutoConfig.limitWidth()
                || this.maxVideoHeight != NarutoConfig.limitHeight()
                || Double.compare(this.bufferSeconds, NarutoConfig.bufferSeconds()) != 0;
    }

    void write() {
        NarutoConfig.set(NarutoConfig.MAX_VIDEO_WIDTH, this.maxVideoWidth);
        NarutoConfig.set(NarutoConfig.MAX_VIDEO_HEIGHT, this.maxVideoHeight);
        NarutoConfig.set(NarutoConfig.AUDIO_VOLUME, this.volume);
        NarutoConfig.set(NarutoConfig.SOURCES_DIR, this.sourcesDir);
        NarutoConfig.set(NarutoConfig.BUFFER_SECONDS, this.bufferSeconds);
        NarutoConfig.set(NarutoConfig.SELECT_MODE, this.selectMode.name().toLowerCase(Locale.ROOT));
        NarutoConfig.set(NarutoConfig.FIXED_NAME, this.fixedName);
        NarutoConfig.set(NarutoConfig.MUTE_MUSIC, this.muteMusic);
        NarutoConfig.set(NarutoConfig.DEBUG_TIME_COSTS, this.debugTimeCosts);
        NarutoConfig.set(NarutoConfig.DEBUG_TIME_COSTS_INTERVAL, this.debugTimeCostsInterval);
        NarutoConfig.set(NarutoConfig.SHIMMER, this.shimmer);
        NarutoConfig.set(NarutoConfig.SHIMMER_SPEED, this.shimmerSpeed);
        NarutoConfig.set(NarutoConfig.SHIMMER_SCALE, this.shimmerScale);
        NarutoConfig.set(NarutoConfig.SHIMMER_STRENGTH, this.shimmerStrength);
        NarutoConfig.set(NarutoConfig.SHIMMER_FPS, this.shimmerFps);
        NarutoConfig.set(NarutoConfig.FADABLE, this.fadable);
        NarutoConfig.set(NarutoConfig.TICKS_BEFORE_FADE, this.ticksBeforeFade);
        NarutoConfig.save();
    }
}
