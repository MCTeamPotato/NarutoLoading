package me.kall.narutoloading.playback.produce.video;

import me.kall.narutoloading.media.ffmpeg.data.VideoArgs;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoloading.playback.produce.AbstractProducer;
import me.kall.narutoloading.playback.produce.util.TimeCosts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
    protected static final long DEFAULT_MAX_BUFFER_BYTES = 512L * 1024L * 1024L;
    private static final int WARM_FRAMES = 8;

    /**
     * Frames travel over a loopback socket instead of the process pipe. A 4K 60fps stream is 746
     * MB/s, and the Windows anonymous pipe hands out only a few KB per read, which turns one frame
     * into thousands of blocking read calls. A socket with a multi-megabyte receive buffer fills a
     * quarter-megabyte read in one call, so the same frame costs a dozen calls.
     */
    private static final int FRAME_SOCKET_BUFFER_BYTES = 4 * 1024 * 1024;
    private static final int FRAME_SOCKET_ACCEPT_TIMEOUT_MILLIS = 500;
    private static final long FRAME_SOCKET_ACCEPT_DEADLINE_NANOS = 10_000_000_000L;
    private static final int READ_BUFFER_BYTES = 256 * 1024;

    private final String ffmpeg;

    public final VideoArgs args;

    private final boolean gpuAccel;

    private final int maxWidth, maxHeight;

    private final TimeCosts onBuiltCosts;
    private final TimeCosts loopCosts;

    private long nextReportNanos;

    private @Nullable ServerSocket frameListener;
    private @Nullable Socket frameSocket;
    private int framePort;

    protected final int frameSize, bufferCapacity;
    protected final int warmFrames;
    protected final LinkedBlockingQueue<Frame<T>> frames;

    private final AtomicReference<T> lastFetched = new AtomicReference<>();

    private final AtomicLong frameIndex = new AtomicLong();

    private final AtomicBoolean waiting = new AtomicBoolean();
    private final AtomicReference<ExecutorService> waitExecutor = new AtomicReference<>();

    private final AtomicBoolean starved = new AtomicBoolean();

    public AbstractFrameProducer(String ffmpeg, VideoArgs args, boolean gpuAccel, int frameSize, double bufferSeconds, int maxWidth, int maxHeight, long maxBufferBytes) {
        this.ffmpeg = ffmpeg;

        this.args = args;
        this.gpuAccel = gpuAccel;

        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;

        this.frameSize = frameSize;
        this.bufferCapacity = bufferCapacity(this.args.fps(), bufferSeconds, frameSize, maxBufferBytes);
        this.warmFrames = Math.min(this.bufferCapacity, WARM_FRAMES);
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

        command.add("tcp://127.0.0.1:" + this.framePort);

        return command.toArray(String[]::new);
    }

    protected abstract String frameType();

    /**
     * Binds the frame socket before the command is built, because ffmpeg is handed the port as its
     * output URL. Skipping this leaves {@code framePort} at zero and the producer does nothing.
     */
    @Override
    public void setup(double seekTo) {
        if (this.ffmpeg == null) return;
        if (!this.openFrameListener()) return;

        super.setup(seekTo);
    }

    @Override
    protected @NotNull InputStream openInput(@NotNull Process process) throws IOException {
        ServerSocket listener = this.frameListener;
        if (listener == null) throw new IOException("Frame listener is not open");

        long deadline = System.nanoTime() + FRAME_SOCKET_ACCEPT_DEADLINE_NANOS;

        while (true) {
            try {
                Socket socket = listener.accept();
                this.frameSocket = socket;

                return socket.getInputStream();
            } catch (SocketTimeoutException timeout) {
                // Report a dead decoder or a wrong port instead of parking this thread forever.
                if (!process.isAlive()) throw new IOException("ffmpeg exited before connecting to the frame socket", timeout);
                if (System.nanoTime() - deadline >= 0L) throw new IOException("Timed out waiting for ffmpeg to connect to the frame socket", timeout);
            }
        }
    }

    private boolean openFrameListener() {
        this.closeFrameTransport();

        try {
            ServerSocket listener = new ServerSocket();

            // Set before binding so the accepted socket inherits it: this is the buffer the decoder
            // writes into, and a few megabytes keeps it from blocking between frames.
            listener.setReceiveBufferSize(FRAME_SOCKET_BUFFER_BYTES);
            listener.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 1);
            listener.setSoTimeout(FRAME_SOCKET_ACCEPT_TIMEOUT_MILLIS);

            this.frameListener = listener;
            this.framePort = listener.getLocalPort();

            return true;
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Could not open the frame socket: " + exception.getMessage());
            return false;
        }
    }

    private void closeFrameTransport() {
        Socket socket = this.frameSocket;
        this.frameSocket = null;

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        ServerSocket listener = this.frameListener;
        this.frameListener = null;

        if (listener != null) {
            try {
                listener.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    protected void forInput(InputStream input) throws IOException, InterruptedException {
        byte[] bufferArray = new byte[Math.min(this.frameSize, READ_BUFFER_BYTES)];

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

    /**
     * Bounds the queue by both the configured seconds and a native-memory budget. A 4K RGBA frame
     * is about 25MB, so a two-second buffer would otherwise mean 3GB of live buffers per producer.
     *
     * @param fps            video frame rate
     * @param seconds        configured buffer duration
     * @param frameSize      bytes per frame
     * @param maxBufferBytes native-memory budget for this producer
     * @return the number of frames to keep
     */
    private static int bufferCapacity(double fps, double seconds, int frameSize, long maxBufferBytes) {
        int bySeconds = Math.max(1, (int) (fps * seconds));
        int byBytes = Math.max(1, (int) (maxBufferBytes / Math.max(1, frameSize)));
        return Math.min(bySeconds, byBytes);
    }

    private void warm() {
        ExecutorService waitExecutor = this.waitExecutor.get();
        if (this.frames.size() >= this.warmFrames && !this.ready.get() && !this.waiting.get() && waitExecutor != null) {
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

        this.closeFrameTransport();

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
