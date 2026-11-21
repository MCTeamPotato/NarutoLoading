package me.kall.narutoloading.media;

/**
 * A video file plus its optional companion audio file (the mod's {@code name.audio.ext} pairing).
 */
public record MediaSource(String video, String audio) {
}
