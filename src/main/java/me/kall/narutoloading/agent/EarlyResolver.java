package me.kall.narutoloading.agent;

import me.kall.narutoloading.agent.bootstrap.EarlyBridge;
import me.kall.narutoloading.agent.bootstrap.EarlyVideo;
import me.kall.narutoloading.media.ffmpeg.data.AudioArgs;
import me.kall.narutoloading.config.NarutoConfig;

import java.net.URL;

public final class EarlyResolver {
    private static volatile EarlyMedia media;

    private EarlyResolver() {
    }

    public static EarlyVideo create(ClassLoader parent) {
        if (parent == null) return null;

        EarlyMedia resolved = media;

        if (resolved == null) {
            try {
                resolved = EarlyMedia.resolve();
            } catch (Throwable throwable) {
                System.err.println("[NarutoLoading Agent] Failed to resolve the early media: " + throwable.getMessage());
                return null;
            }

            if (resolved == null) {
                System.out.println("[NarutoLoading Agent] No early media resolved, early video disabled");
                return null;
            }

            media = resolved;
            System.setProperty(EarlyBridge.MEDIA_PROPERTY, resolved.video.absPath());
            System.out.println("[NarutoLoading Agent] Early media: " + resolved.video.absPath() + " (" + resolved.video.scaledWidth(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()) + "x" + resolved.video.scaledHeight(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()) + ")");
        }

        try {
            EarlyClassLoader loader = new EarlyClassLoader(new URL[]{EarlyResolver.class.getProtectionDomain().getCodeSource().getLocation()}, parent);

            AudioArgs audio = resolved.audio;

            return (EarlyVideo) Class.forName("me.kall.narutoloading.agent.EarlyVideoImpl", true, loader).getMethod("create", String.class, String.class, int.class, int.class, double.class, double.class, double.class, String.class, int.class, int.class, double.class, double.class, float.class).invoke(null,
                    resolved.ffmpeg,
                    resolved.video.absPath(),
                    resolved.video.scaledWidth(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()),
                    resolved.video.scaledHeight(NarutoConfig.limitWidth(), NarutoConfig.limitHeight()),
                    resolved.video.fps(),
                    resolved.video.duration(),
                    resolved.video.startSec(),
                    audio == null ? null : audio.absPath(),
                    audio == null ? 0 : audio.channels(),
                    audio == null ? 0 : audio.sampleRate(),
                    audio == null ? 0D : audio.startSec(),
                    audio == null ? 0D : audio.duration(),
                    resolved.volume);
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading Agent] Failed to create the early video: " + throwable.getMessage());
            return null;
        }
    }
}
