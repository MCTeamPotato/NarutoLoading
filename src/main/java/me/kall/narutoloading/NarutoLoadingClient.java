package me.kall.narutoloading;

import me.kall.narutoloading.core.NarutoRenderer;
import me.kall.narutoloading.executor.NarutoAudioExecutor;
import me.kall.narutoloading.executor.NarutoVideoExecutor;

public class NarutoLoadingClient {
    public static final NarutoAudioExecutor AUDIO = new NarutoAudioExecutor();
    public static final NarutoVideoExecutor VIDEO = new NarutoVideoExecutor();
    public static final NarutoRenderer RENDERER = new NarutoRenderer();
}
