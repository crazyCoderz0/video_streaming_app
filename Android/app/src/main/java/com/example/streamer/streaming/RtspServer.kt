package com.example.streamer.streaming

import android.content.Context
import com.example.streamer.domain.EncodedFrame
import com.example.streamer.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class RtspServer(private val context: Context, private val port: Int) : Closeable {
    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var scope: CoroutineScope? = null
    private val sessions = CopyOnWriteArrayList<RtpSession>()

    fun start() {
        if (running.getAndSet(true)) return
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = newScope
        serverSocket = ServerSocket(port)
        newScope.launch {
            while (running.get()) {
                runCatching { serverSocket?.accept() }
                    .onSuccess { socket -> if (socket != null) handleClient(socket) }
                    .onFailure { if (running.get()) Timber.e(it, "RTSP accept failed") }
            }
        }
    }

    fun submit(frame: EncodedFrame) {
        sessions.forEach { it.send(frame) }
    }

    fun endpoint(): String = "rtsp://${NetworkUtils.localIpv4().ifBlank { "device-ip" }}:$port/live"

    override fun close() = stop()

    fun stop() {
        running.set(false)
        sessions.forEach { it.close() }
        sessions.clear()
        runCatching { serverSocket?.close() }
        scope?.cancel()
        scope = null
    }

    private fun handleClient(socket: Socket) {
        scope?.launch {
            socket.use { client ->
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val writer = client.getOutputStream()
                var session: RtpSession? = null
                while (running.get() && !client.isClosed) {
                    val requestLine = reader.readLine() ?: break
                    if (requestLine.isBlank()) continue
                    val headers = mutableMapOf<String, String>()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isBlank()) break
                        val index = line.indexOf(':')
                        if (index > 0) headers[line.substring(0, index).trim()] = line.substring(index + 1).trim()
                    }
                    val cseq = headers["CSeq"] ?: "1"
                    val method = requestLine.substringBefore(' ')
                    val response = when (method) {
                        "OPTIONS" -> "RTSP/1.0 200 OK\r\nCSeq: $cseq\r\nPublic: OPTIONS, DESCRIBE, SETUP, PLAY, TEARDOWN\r\n\r\n"
                        "DESCRIBE" -> describe(cseq)
                        "SETUP" -> {
                            val transport = headers["Transport"].orEmpty()
                            val clientPort = Regex("client_port=(\\d+)").find(transport)?.groupValues?.get(1)?.toIntOrNull() ?: 5004
                            session = RtpSession(client.inetAddress.hostAddress, clientPort).also { sessions.add(it) }
                            "RTSP/1.0 200 OK\r\nCSeq: $cseq\r\nTransport: RTP/AVP;unicast;client_port=$clientPort-${clientPort + 1};server_port=5006-5007\r\nSession: 1\r\n\r\n"
                        }
                        "PLAY" -> "RTSP/1.0 200 OK\r\nCSeq: $cseq\r\nSession: 1\r\nRTP-Info: url=rtsp://${NetworkUtils.localIpv4()}:$port/live/trackID=0\r\n\r\n"
                        "TEARDOWN" -> {
                            session?.let { sessions.remove(it); it.close() }
                            "RTSP/1.0 200 OK\r\nCSeq: $cseq\r\nSession: 1\r\n\r\n"
                        }
                        else -> "RTSP/1.0 405 Method Not Allowed\r\nCSeq: $cseq\r\n\r\n"
                    }
                    writer.write(response.toByteArray())
                    writer.flush()
                    if (method == "TEARDOWN") break
                }
            }
        }
    }

    private fun describe(cseq: String): String {
        val sdp = """
            v=0
            o=- 0 0 IN IP4 ${NetworkUtils.localIpv4()}
            s=Android Camera Stream
            c=IN IP4 0.0.0.0
            t=0 0
            m=video 0 RTP/AVP 96
            a=rtpmap:96 H264/90000
            a=control:trackID=0
        """.trimIndent().replace("\n", "\r\n")
        return "RTSP/1.0 200 OK\r\nCSeq: $cseq\r\nContent-Type: application/sdp\r\nContent-Length: ${sdp.toByteArray().size}\r\n\r\n$sdp"
    }
}

private class RtpSession(host: String, private val port: Int) : Closeable {
    private val socket = DatagramSocket()
    private val address = java.net.InetAddress.getByName(host)
    private var sequence = 0
    private val ssrc = 0x13572468

    fun send(frame: EncodedFrame) {
        if (frame.isConfig) return
        val timestamp = (frame.timestampUs * 90L).toInt()
        var offset = 0
        while (offset < frame.bytes.size) {
            val chunk = minOf(1200, frame.bytes.size - offset)
            val packet = ByteArray(12 + chunk)
            packet[0] = 0x80.toByte()
            packet[1] = 96.toByte()
            packet[2] = (sequence shr 8).toByte()
            packet[3] = sequence.toByte()
            packet[4] = (timestamp shr 24).toByte()
            packet[5] = (timestamp shr 16).toByte()
            packet[6] = (timestamp shr 8).toByte()
            packet[7] = timestamp.toByte()
            packet[8] = (ssrc shr 24).toByte()
            packet[9] = (ssrc shr 16).toByte()
            packet[10] = (ssrc shr 8).toByte()
            packet[11] = ssrc.toByte()
            System.arraycopy(frame.bytes, offset, packet, 12, chunk)
            socket.send(DatagramPacket(packet, packet.size, address, port))
            sequence = (sequence + 1) and 0xFFFF
            offset += chunk
        }
    }

    override fun close() {
        socket.close()
    }
}

