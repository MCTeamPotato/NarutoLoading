package me.kall.narutoloading;

import me.kall.narutoloading.executor.NarutoAudioExecutor;
import me.kall.narutoloading.executor.NarutoVideoExecutor;
import me.kall.narutoloading.render.NarutoRenderer;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(NarutoLoading.MOD_ID)
public final class NarutoLoading {
    public static final String MOD_ID = "narutoloading";

    public static final Logger LOGGER = LogManager.getLogger(NarutoLoading.class);

    public static final NarutoAudioExecutor AUDIO = new NarutoAudioExecutor();
    public static final NarutoVideoExecutor VIDEO = new NarutoVideoExecutor();

    public static final NarutoRenderer RENDERER = new NarutoRenderer();
}
