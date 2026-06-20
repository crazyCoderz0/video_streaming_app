package com.example.receiver.controller;

import com.example.receiver.decoder.FfmpegH264Decoder;
import com.example.receiver.network.SocketVideoServer;
import com.example.receiver.network.VideoPacket;
import com.example.receiver.utils.FpsCounter;
import com.example.receiver.utils.LogSink;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class ReceiverController implements AutoCloseable {
    private final Consumer<Image> frameConsumer;
    private final Consumer<String> statusConsumer;
    private final Consumer<Double> fpsConsumer;
    private final Consumer<Long> latencyConsumer;
    private final LogSink logger;
    private final FpsCounter fpsCounter = new FpsCounter();
    private final AtomicLong lastLatencyMs = new AtomicLong();
    private SocketVideoServer server;
    private FfmpegH264Decoder decoder;

    public ReceiverController(
        Consumer<Image> frameConsumer,
        Consumer<String> statusConsumer,
        Consumer<Double> fpsConsumer,
        Consumer<Long> latencyConsumer,
        LogSink logger
    ) {
        this.frameConsumer = frameConsumer;
        this.statusConsumer = statusConsumer;
        this.fpsConsumer = fpsConsumer;
        this.latencyConsumer = latencyConsumer;
        this.logger = logger;
    }

    public void start(int port) throws IOException {
        stop();
        decoder = new FfmpegH264Decoder(image -> Platform.runLater(() -> {
            frameConsumer.accept(image);
            fpsConsumer.accept(fpsCounter.tick());
            latencyConsumer.accept(lastLatencyMs.get());
        }), logger);
        decoder.start();
        server = new SocketVideoServer(port, this::onPacket, logger);
        server.start();
        Platform.runLater(() -> statusConsumer.accept("Listening on " + port));
    }

    private void onPacket(VideoPacket packet) {
        long nowUs = System.nanoTime() / 1_000L;
        lastLatencyMs.set(Math.max(0, (nowUs - packet.timestampUs()) / 1000));
        decoder.decode(packet);
    }

    public void stop() {
        if (server != null) server.close();
        if (decoder != null) decoder.close();
        Platform.runLater(() -> statusConsumer.accept("Stopped"));
    }

    @Override
    public void close() {
        stop();
    }
}

