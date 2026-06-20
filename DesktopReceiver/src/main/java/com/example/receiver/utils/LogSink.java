package com.example.receiver.utils;

@FunctionalInterface
public interface LogSink {
    void log(String message);
}

