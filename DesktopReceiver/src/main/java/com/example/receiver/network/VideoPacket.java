package com.example.receiver.network;

public record VideoPacket(byte flags, long timestampUs, byte[] payload) {
    public boolean keyFrame() {
        return (flags & 1) == 1;
    }
}

