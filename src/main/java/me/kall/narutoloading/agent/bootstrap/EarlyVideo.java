package me.kall.narutoloading.agent.bootstrap;

@SuppressWarnings("unused")
public interface EarlyVideo {
    void present(boolean flipY);

    double positionSec();

    boolean finished();

    void close();
}
