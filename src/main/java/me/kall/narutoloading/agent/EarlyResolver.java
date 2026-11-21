package me.kall.narutoloading.agent;

import me.kall.narutoloading.agent.bootstrap.EarlyVideo;
import me.kall.narutoloading.app.ffmpeg.data.AudioArgs;

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
            System.setProperty("narutoloading.early.media", resolved.video.absPath());
            System.out.println("[NarutoLoading Agent] Early media: " + resolved.video.absPath() + " (" + resolved.video.width() + "x" + resolved.video.height() + ")");
        }

        try {
            EarlyClassLoader loader = new EarlyClassLoader(new URL[]{EarlyResolver.class.getProtectionDomain().getCodeSource().getLocation()}, parent);

            AudioArgs audio = resolved.audio;

            return (EarlyVideo) Class.forName("me.kall.narutoloading.agent.EarlyVideoImpl", true, loader).getMethod("create", String.class, String.class, int.class, int.class, double.class, double.class, double.class, String.class, int.class, int.class, int.class, double.class, double.class, float.class, double.class).invoke(null,
                    resolved.ffmpeg,
                    resolved.video.absPath(),
                    resolved.video.width(),
                    resolved.video.height(),
                    resolved.video.fps(),
                    resolved.video.duration(),
                    resolved.video.startSec(),
                    audio == null ? null : audio.absPath(),
                    audio == null ? 0 : audio.channels(),
                    audio == null ? 0 : audio.sampleRate(),
                    audio == null ? 0 : audio.openALFormat(),
                    audio == null ? 0D : audio.startSec(),
                    audio == null ? 0D : audio.duration(),
                    resolved.volume,
                    resolved.bufferSeconds);
        } catch (Throwable throwable) {
            System.err.println("[NarutoLoading Agent] Failed to create the early video: " + throwable.getMessage());
            return null;
        }
    }
}
