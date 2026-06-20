package com.example.receiver.decoder;

import com.example.receiver.utils.LogSink;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

final class MjpegReader implements Runnable {
    private final InputStream input;
    private final Consumer<Image> imageConsumer;
    private final LogSink logger;

    MjpegReader(InputStream input, Consumer<Image> imageConsumer, LogSink logger) {
        this.input = input;
        this.imageConsumer = imageConsumer;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                byte[] jpeg = nextJpeg();
                if (jpeg.length > 0) {
                    imageConsumer.accept(new Image(new ByteArrayInputStream(jpeg)));
                }
            }
        } catch (IOException e) {
            logger.log("MJPEG reader stopped: " + e.getMessage());
        }
    }

    private byte[] nextJpeg() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(128 * 1024);
        int previous = -1;
        boolean inImage = false;
        int current;
        while ((current = input.read()) != -1) {
            if (!inImage && previous == 0xFF && current == 0xD8) {
                inImage = true;
                out.write(0xFF);
            }
            if (inImage) out.write(current);
            if (inImage && previous == 0xFF && current == 0xD9) {
                return out.toByteArray();
            }
            previous = current;
        }
        throw new IOException("End of decoder stream");
    }
}

