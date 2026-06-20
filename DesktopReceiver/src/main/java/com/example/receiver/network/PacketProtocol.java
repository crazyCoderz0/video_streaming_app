package com.example.receiver.network;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

public final class PacketProtocol {
    public static final int MAGIC = 0x4C565331;
    public static final int MAX_FRAME_BYTES = 2 * 1024 * 1024;

    private PacketProtocol() {}

    public static VideoPacket read(DataInputStream input) throws IOException {
        int magic = input.readInt();
        if (magic != MAGIC) {
            throw new IOException("Malformed packet magic");
        }
        int version = input.readUnsignedByte();
        if (version != 1) {
            throw new IOException("Unsupported protocol version " + version);
        }
        byte flags = input.readByte();
        long timestamp = input.readLong();
        int length = input.readInt();
        if (length <= 0 || length > MAX_FRAME_BYTES) {
            throw new IOException("Invalid frame length " + length);
        }
        byte[] payload = input.readNBytes(length);
        if (payload.length != length) {
            throw new EOFException("Socket closed mid-frame");
        }
        return new VideoPacket(flags, timestamp, payload);
    }
}

