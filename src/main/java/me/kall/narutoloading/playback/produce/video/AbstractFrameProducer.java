package me.kall.narutoloading.playback.produce.video;

import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.playback.produce.AbstractProducer;
import me.kall.narutoloading.playback.produce.util.TimeCosts;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractFrameProducer<T> extends AbstractProducer {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final String ffmpeg;

    public final VideoArgs args;

    private final boolean gpuAccel;

    private final int maxWidth, maxHeight;

    private final TimeCosts onBuiltCosts;
    private final TimeCosts loopCosts;

    private long nextReportNanos;

    protected final int frameSize, bufferCapacity;
    protected final LinkedBlockingQueue<Frame<T>> frames;

    private final AtomicReference<T> lastFetched = new AtomicReference<>();

    private final AtomicLong frameIndex = new AtomicLong();

    private final AtomicBoolean waiting = new AtomicBoolean();
    private final AtomicReference<ExecutorService> waitExecutor = new AtomicReference<>();

    private final AtomicBoolean starved = new AtomicBoolean();

    public AbstractFrameProducer(String ffmpeg, VideoArgs args, boolean gpuAccel, int frameSize, double bufferSeconds) {
        this(ffmpeg, args, gpuAccel, frameSize, bufferSeconds, NarutoConfig.limitWidth(), NarutoConfig.limitHeight());
    }

    public AbstractFrameProducer(String ffmpeg, VideoArgs args, boolean gpuAccel, int frameSize, double bufferSeconds, int maxWidth, int maxHeight) {
        this.ffmpeg = ffmpeg;

        this.args = args;
        this.gpuAccel = gpuAccel;

        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;

        this.frameSize = frameSize;
        this.bufferCapacity = (int) (this.args.fps() * bufferSeconds);
        this.frames = new LinkedBlockingQueue<>(this.bufferCapacity);
        this.nextReportNanos = System.nanoTime() + reportIntervalNanos();

        this.onBuiltCosts = new TimeCosts("onBuilt", this.bufferCapacity / 2);
        this.loopCosts = new TimeCosts("frameLoop", this.bufferCapacity / 2);
    }

    public int scaledWidth() {
        return this.args.scaledWidth(this.maxWidth, this.maxHeight);
    }

    public int scaledHeight() {
        return this.args.scaledHeight(this.maxWidth, this.maxHeight);
    }

    public int bufferedFrames() {
        return this.frames.size();
    }

    @Override
    public AbstractFrameProducer<T> withWarmListener(Runnable warmListener) {
        super.withWarmListener(warmListener);
        return this;
    }

    public @Nullable Frame<T> fetch(double positionSec) {
        if (!this.ready.get()) return null;

        this.removeLast();

        long expected = (long) (positionSec * this.args.fps());

        Frame<T> frame = this.frames.poll();

        while (frame != null && frame.index < expected) {
            this.onDead(frame.data);
            frame = this.frames.poll();
        }

        if (frame == null) {
            this.starved.set(true);
        } else {
            this.lastFetched.set(frame.data);
        }

        return frame;
    }

    public boolean consumeStarvation() {
        if (!this.starved.getAndSet(false)) return false;

        this.reportTimeCosts("starved");

        return true;
    }

    private void removeLast() {
        T lastFetched = this.lastFetched.getAndSet(null);
        if (lastFetched != null) this.onDead(lastFetched);
    }

    @Override
    protected @Nullable String[] setCommand(double seekTo) {
        if (this.ffmpeg == null) return null;

        List<String> command = new ArrayList<>();

        command.add(this.ffmpeg);
        command.add("-nostdin");

        if (this.gpuAccel) {
            command.add("-hwaccel");
            command.add("auto");
        }

        command.add("-threads");
        command.add("0");

        command.add("-ss");
        command.add(String.valueOf(seekTo + this.args.startSec()));

        command.add("-i");
        command.add(this.args.absPath());

        command.add("-map");
        command.add("0:v:0");

        command.add("-an");
        command.add("-sn"); command.add("-dn");

        command.add("-vf");
        command.add("scale=" + this.scaledWidth() + ":" + this.scaledHeight() + ":flags=fast_bilinear,format=" + this.frameType());

        command.add("-f");
        command.add("rawvideo");

        command.add("-vcodec");
        command.add("rawvideo");

        command.add("-loglevel");
        command.add("error");

        command.add("-");

        return command.toArray(String[]::new);
    }

    protected abstract String frameType();

    @Override
    protected void forInput(InputStream input) throws IOException, InterruptedException {
        byte[] bufferArray = new byte[Math.min(this.frameSize, 64 * 1024)];

        while (!this.off.get()) {
            long loopStart = System.nanoTime();

            ByteBuffer frame = this.next();
            frame.clear();

            while (frame.hasRemaining()) {
                int read = input.read(bufferArray, 0, Math.min(bufferArray.length, frame.remaining()));
                if (read == -1) return;
                frame.put(bufferArray, 0, read);
            }

            long builtStart = System.nanoTime();
            this.onBuilt(frame.flip(), this.frameIndex.incrementAndGet());
            this.onBuiltCosts.record(System.nanoTime() - builtStart);

            this.warm();

            this.loopCosts.record(System.nanoTime() - loopStart);

            this.reportIfDue();
        }
    }

    private void reportIfDue() {
        if (!NarutoConfig.debugTimeCosts()) return;

        long now = System.nanoTime();
        if (now - this.nextReportNanos < 0L) return;

        this.nextReportNanos = now + reportIntervalNanos();
        this.reportTimeCosts("periodic");
    }

    private void reportTimeCosts(String reason) {
        System.err.println("[NarutoLoading] Time costs (" + reason + "): " + this.onBuiltCosts.describe() + " | " + this.loopCosts.describe());
        this.onBuiltCosts.reset();
        this.loopCosts.reset();
    }

    private static long reportIntervalNanos() {
        return Math.max(1, NarutoConfig.debugTimeCostsInterval()) * NANOS_PER_SECOND;
    }

    private void warm() {
        ExecutorService waitExecutor = this.waitExecutor.get();
        if (this.frames.remainingCapacity() == 0 && !this.ready.get() && !this.waiting.get() && waitExecutor != null) {
            this.waiting.set(true);
            CompletableFuture.runAsync(() -> {
                if (this.awaitEager()) {
                    this.notifyWarmListener();

                    this.ready.set(true);
                }
            }, waitExecutor).whenComplete((unused, throwable) -> {
                if (throwable != null) System.err.println("[NarutoLoading] Video warmup task failed: " + throwable.getMessage());
                waitExecutor.shutdown();
                this.waitExecutor.set(null);
            });
        }
    }

    protected abstract ByteBuffer next();

    protected abstract void onBuilt(ByteBuffer frame, long frameIndex) throws InterruptedException;
    protected abstract void onDead(T frame);

    @Override
    protected void onSetup(double seekTo) {
        this.frameIndex.set((long)(seekTo * this.args.fps()));

        ExecutorService waitExecutor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "NarutoWaiter_" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        });

        this.waitExecutor.set(waitExecutor);
        this.waiting.set(false);
    }

    @Override
    public void shutdown() {
        super.shutdown();

        ExecutorService waitExecutor = this.waitExecutor.getAndSet(null);
        if (waitExecutor != null) waitExecutor.shutdown();

        for (Frame<T> frame : this.frames) {
            this.onDead(frame.data);
        }

        this.frames.clear();

        this.removeLast();
    }

    public record Frame<T>(T data, long index) {}
}
