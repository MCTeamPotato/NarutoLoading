package me.kall.narutoloading.playback.early;

public interface PlaybackSession {
    void present(boolean flipY);

    boolean finished();

    /**
     * Returns the playback position in seconds, so a restart can continue where the borrowed
     * session currently is instead of jumping back to the start.
     *
     * @return the current position in seconds
     */
    double positionSec();

    void close();
}
