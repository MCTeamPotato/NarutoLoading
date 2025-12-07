package me.kall.narutoloading;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class NarutoVideoPlayer {
    private static final String FFMPEG_PATH = "D:\\ffmpeg\\bin\\ffmpeg.exe";
    private static final String VIDEO_PATH = "config/naruto.mp4";

    private static DynamicTexture dynamicTexture;
    private static ResourceLocation textureLocation;

    private static final BlockingQueue<NativeImage> frameQueue = new LinkedBlockingQueue<>(120);
    private static volatile boolean cancelled = false;

    private static ExecutorService executor;

    public static void init() {
        if (dynamicTexture != null) return;

        dynamicTexture = new DynamicTexture(854, 480, false);
        textureLocation = Minecraft.getInstance().getTextureManager().register("naruto_video_dynamic", dynamicTexture);

        startAsyncLoader();
        NarutoAudioPlayer.init();
    }

    private static void startAsyncLoader() {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Naruto-Video-Loader");
            t.setDaemon(true);
            return t;
        });
        executor.submit(() -> {
            Process process = null;
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        FFMPEG_PATH, "-i", VIDEO_PATH,
                        "-vf", "format=rgb24,scale=854:480",
                        "-pix_fmt", "rgb24",
                        "-f", "image2pipe",
                        "-vcodec", "rawvideo",
                        "-loglevel", "error",
                        "-"
                );

                process = pb.start();
                InputStream in = process.getInputStream();

                byte[] buffer = new byte[854 * 480 * 3];
                int frameSize = buffer.length;

                while (!cancelled) {
                    int read = 0;
                    while (read < frameSize) {
                        int r = in.read(buffer, read, frameSize - read);
                        if (r == -1) {
                            return;
                        }
                        read += r;
                    }

                    NativeImage image = getNativeImage(buffer);

                    frameQueue.put(image);

                    while (frameQueue.size() > 100) {
                        Thread.sleep(1);
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            } finally {
                if (process != null) process.destroyForcibly();
            }
        });
    }

    private static @NotNull NativeImage getNativeImage(byte @NotNull [] buffer) {
        NativeImage image = new NativeImage(854, 480, false);
        for (int i = 0; i < buffer.length; i += 3) {
            int b = buffer[i] & 0xFF;
            int g = buffer[i + 1] & 0xFF;
            int r = buffer[i + 2] & 0xFF;
            int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
            image.setPixelRGBA(i/3 % 854, i/3 / 854, argb);
        }
        return image;
    }

    private static long lastFrameTime = 0;
    private static final int TARGET_FPS = 30;

    public static ResourceLocation getCurrentFrameTexture() {
        if (dynamicTexture == null) init();

        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= 1000 / TARGET_FPS) {
            lastFrameTime = now;

            NativeImage frame = frameQueue.poll();
            if (frame != null) {
                dynamicTexture.setPixels(frame);
                dynamicTexture.upload();
                frame.close();
            }
        }

        return textureLocation;
    }

    public static void shutdown() {
        cancelled = true;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }
        frameQueue.clear();
        if (dynamicTexture != null) {
            dynamicTexture.close();
        }
        NarutoAudioPlayer.shutdown();
    }
}