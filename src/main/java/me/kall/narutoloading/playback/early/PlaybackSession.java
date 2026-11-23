package me.kall.narutoloading.playback.early;

public interface PlaybackSession {
    void present(boolean flipY);

    boolean finished();

    double positionSec();

    double durationSec();

    void close();
}
