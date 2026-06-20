package com.example.receiver.network;

import com.example.receiver.utils.LogSink;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class SocketVideoServer implements AutoCloseable {
    private final int port;
    private final Consumer<VideoPacket> packetConsumer;
    private final LogSink logger;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;

    public SocketVideoServer(int port, Consumer<VideoPacket> packetConsumer, LogSink logger) {
        this.port = port;
        this.packetConsumer = Objects.requireNonNull(packetConsumer);
        this.logger = Objects.requireNonNull(logger);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor.submit(() -> {
            try (ServerSocket server = new ServerSocket(port)) {
                server.setReuseAddress(true);
                this.serverSocket = server;
                logger.log("Receiver listening on port " + port);
                while (running.get()) {
                    Socket socket = server.accept();
                    socket.setTcpNoDelay(true);
                    socket.setSoTimeout((int) Duration.ofSeconds(15).toMillis());
                    executor.submit(() -> handleClient(socket));
                }
            } catch (IOException e) {
                if (running.get()) logger.log("Server stopped: " + e.getMessage());
            }
        });
    }

    private void handleClient(Socket socket) {
        logger.log("Android connected: " + socket.getRemoteSocketAddress());
        try (socket; DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 256 * 1024))) {
            while (running.get()) {
                packetConsumer.accept(PacketProtocol.read(input));
            }
        } catch (IOException e) {
            logger.log("Client disconnected: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        running.set(false);
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        executor.shutdownNow();
    }
}

