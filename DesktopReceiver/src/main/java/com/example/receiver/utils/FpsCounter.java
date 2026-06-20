package com.example.receiver.utils;

public final class FpsCounter {
    private long windowStart = System.nanoTime();
    private int frames;
    private double current;

    public double tick() {
        frames++;
        long now = System.nanoTime();
        long elapsed = now - windowStart;
        if (elapsed >= 1_000_000_000L) {
            current = frames * 1_000_000_000.0 / elapsed;
            frames = 0;
            windowStart = now;
        }
        return current;
    }
}

