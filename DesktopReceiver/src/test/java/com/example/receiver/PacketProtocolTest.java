package com.example.receiver;

import com.example.receiver.network.PacketProtocol;
import com.example.receiver.network.VideoPacket;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketProtocolTest {
    @Test
    void readsValidPacket() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(PacketProtocol.MAGIC);
        out.writeByte(1);
        out.writeByte(1);
        out.writeLong(42L);
        out.writeInt(3);
        out.write(new byte[] {1, 2, 3});
        VideoPacket packet = PacketProtocol.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        assertTrue(packet.keyFrame());
        assertEquals(42L, packet.timestampUs());
        assertEquals(3, packet.payload().length);
    }
}

