package me.kall.narutoloading.playback.early;

public interface PlaybackSession {
    void present(boolean flipY);

    boolean finished();

    void close();
}
