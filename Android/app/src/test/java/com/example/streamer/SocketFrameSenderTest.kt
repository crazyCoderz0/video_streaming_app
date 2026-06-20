package com.example.streamer

import com.example.streamer.domain.EncodedFrame
import com.example.streamer.streaming.SocketFrameSender
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.DataInputStream
import java.net.ServerSocket
import kotlin.concurrent.thread

class SocketFrameSenderTest {
    @Test
    fun sendsLengthPrefixedFrame() {
        ServerSocket(0).use { server ->
            val received = mutableListOf<Int>()
            val worker = thread {
                server.accept().use { socket ->
                    val input = DataInputStream(socket.getInputStream())
                    received += input.readInt()
                    input.readByte()
                    input.readByte()
                    input.readLong()
                    received += input.readInt()
                }
            }
            SocketFrameSender().use { sender ->
                sender.connect("127.0.0.1", server.localPort)
                sender.send(EncodedFrame(byteArrayOf(1, 2, 3), 1L, true))
            }
            worker.join(1000)
            assertEquals(SocketFrameSender.MAGIC, received[0])
            assertEquals(3, received[1])
        }
    }
}

