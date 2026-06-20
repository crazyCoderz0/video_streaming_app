package com.example.receiver.utils;

import nu.pattern.OpenCV;
import org.opencv.core.Core;

public final class OpenCvBootstrap {
    private OpenCvBootstrap() {}

    public static void load(LogSink logger) {
        try {
            OpenCV.loadLocally();
            logger.log("OpenCV loaded: " + Core.VERSION);
        } catch (RuntimeException error) {
            logger.log("OpenCV unavailable, FFmpeg decoder remains active: " + error.getMessage());
        }
    }
}

