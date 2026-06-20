package com.example.streamer.utils

import com.example.streamer.domain.StreamTarget
import java.net.InetAddress

object InputValidator {
    fun parseTarget(host: String, portText: String): Result<StreamTarget> = runCatching {
        val normalizedHost = host.trim()
        require(normalizedHost.length <= 253) { "IP address is too long" }
        val address = InetAddress.getByName(normalizedHost)
        require(!address.isAnyLocalAddress) { "Use the desktop receiver IP address" }
        val port = portText.trim().toInt()
        require(port in 1024..65535) { "Port must be between 1024 and 65535" }
        StreamTarget(address.hostAddress ?: normalizedHost, port)
    }
}

