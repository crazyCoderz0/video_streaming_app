package com.example.streamer.domain

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val host: String, val port: Int) : ConnectionState
    data class Failed(val message: String) : ConnectionState
}

sealed interface StreamingState {
    data object Stopped : StreamingState
    data object Starting : StreamingState
    data object Streaming : StreamingState
    data class Failed(val message: String) : StreamingState
}

data class UiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val streamingState: StreamingState = StreamingState.Stopped,
    val rtspEndpoint: String = "",
    val error: ErrorState? = null
)

data class ErrorState(val reason: ErrorReason, val message: String)

enum class ErrorReason {
    NETWORK_LOST,
    SOCKET_CLOSED,
    ENCODER_FAILURE,
    CAMERA_FAILURE,
    PERMISSION_DENIED,
    DEVICE_ROTATION,
    APP_BACKGROUNDED,
    LOW_MEMORY,
    VALIDATION
}

data class StreamTarget(val host: String, val port: Int)

data class EncodedFrame(
    val bytes: ByteArray,
    val timestampUs: Long,
    val isKeyFrame: Boolean,
    val isConfig: Boolean = false
)

