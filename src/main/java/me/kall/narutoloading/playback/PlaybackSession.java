package me.kall.narutoloading.playback;

public interface PlaybackSession {
    void present(boolean flipY);

    boolean finished();

    void close();
}
