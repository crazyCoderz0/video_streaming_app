package com.example.receiver.decoder;

import com.example.receiver.network.VideoPacket;
import com.example.receiver.utils.LogSink;
import javafx.scene.image.Image;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class FfmpegH264Decoder implements AutoCloseable {
    private final Consumer<Image> imageConsumer;
    private final LogSink logger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Process process;
    private OutputStream stdin;

    public FfmpegH264Decoder(Consumer<Image> imageConsumer, LogSink logger) {
        this.imageConsumer = imageConsumer;
        this.logger = logger;
    }

    public synchronized void start() throws IOException {
        if (!running.compareAndSet(false, true)) return;
        process = new ProcessBuilder(
            "ffmpeg", "-loglevel", "error",
            "-fflags", "nobuffer",
            "-flags", "low_delay",
            "-f", "h264",
            "-i", "pipe:0",
            "-f", "image2pipe",
            "-vcodec", "mjpeg",
            "pipe:1"
        ).redirectErrorStream(true).start();
        stdin = process.getOutputStream();
        executor.submit(new MjpegReader(process.getInputStream(), imageConsumer, logger));
        logger.log("FFmpeg decoder started");
    }

    public synchronized void decode(VideoPacket packet) {
        if (!running.get() || stdin == null) return;
        try {
            stdin.write(packet.payload());
            stdin.flush();
        } catch (IOException e) {
            logger.log("Decoder write failed: " + e.getMessage());
            close();
        }
    }

    @Override
    public synchronized void close() {
        running.set(false);
        try {
            if (stdin != null) stdin.close();
        } catch (IOException ignored) {
        }
        if (process != null) process.destroyForcibly();
        executor.shutdownNow();
    }
}

