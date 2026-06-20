package com.example.streamer.streaming

import com.example.streamer.domain.EncodedFrame
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class SocketFrameSender : Closeable {
    private var socket: Socket? = null
    private var output: DataOutputStream? = null
    private val open = AtomicBoolean(false)

    @Synchronized
    fun connect(host: String, port: Int) {
        close()
        val newSocket = Socket()
        newSocket.tcpNoDelay = true
        newSocket.keepAlive = true
        newSocket.connect(InetSocketAddress(host, port), 5_000)
        socket = newSocket
        output = DataOutputStream(BufferedOutputStream(newSocket.getOutputStream(), 256 * 1024))
        open.set(true)
    }

    @Synchronized
    fun send(frame: EncodedFrame) {
        val out = output ?: error("Socket is not connected")
        require(frame.bytes.size <= MAX_FRAME_BYTES) { "Frame too large" }
        out.writeInt(MAGIC)
        out.writeByte(VERSION)
        out.writeByte(if (frame.isKeyFrame) 1 else 0)
        out.writeLong(frame.timestampUs)
        out.writeInt(frame.bytes.size)
        out.write(frame.bytes)
        out.flush()
    }

    override fun close() {
        open.set(false)
        runCatching { output?.close() }
        runCatching { socket?.close() }
        output = null
        socket = null
    }

    companion object {
        const val MAGIC = 0x4C565331
        const val VERSION = 1
        const val MAX_FRAME_BYTES = 2 * 1024 * 1024
    }
}

