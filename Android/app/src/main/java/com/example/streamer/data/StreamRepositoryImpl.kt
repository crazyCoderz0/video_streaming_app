package com.example.streamer.data

import com.example.streamer.domain.ConnectionState
import com.example.streamer.domain.EncodedFrame
import com.example.streamer.domain.ErrorReason
import com.example.streamer.domain.ErrorState
import com.example.streamer.domain.StreamRepository
import com.example.streamer.domain.StreamTarget
import com.example.streamer.domain.StreamingState
import com.example.streamer.domain.UiState
import com.example.streamer.streaming.RtspServer
import com.example.streamer.streaming.SocketFrameSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepositoryImpl @Inject constructor(
    private val socketSender: SocketFrameSender,
    private val rtspServer: RtspServer
) : StreamRepository {
    private val _uiState = MutableStateFlow(UiState())
    override val uiState: StateFlow<UiState> = _uiState

    override suspend fun connect(target: StreamTarget) {
        withContext(Dispatchers.IO) {
            _uiState.update { it.copy(connectionState = ConnectionState.Connecting, error = null) }
            runCatching { socketSender.connect(target.host, target.port) }
                .onSuccess { _uiState.update { it.copy(connectionState = ConnectionState.Connected(target.host, target.port)) } }
                .onFailure { failConnection(it) }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            socketSender.close()
            _uiState.update { it.copy(connectionState = ConnectionState.Disconnected, streamingState = StreamingState.Stopped) }
        }
    }

    override suspend fun startStreaming() {
        if (_uiState.value.connectionState !is ConnectionState.Connected) {
            _uiState.update { it.copy(error = ErrorState(ErrorReason.SOCKET_CLOSED, "Connect to the desktop receiver first")) }
            return
        }
        _uiState.update { it.copy(streamingState = StreamingState.Starting, error = null) }
        _uiState.update { it.copy(streamingState = StreamingState.Streaming) }
    }

    override suspend fun stopStreaming() {
        _uiState.update { it.copy(streamingState = StreamingState.Stopped) }
    }

    override suspend fun sendFrame(frame: EncodedFrame) {
        withContext(Dispatchers.IO) {
            if (_uiState.value.streamingState != StreamingState.Streaming) return@withContext
            runCatching {
                socketSender.send(frame)
                rtspServer.submit(frame)
            }.onFailure {
                Timber.e(it, "Frame send failed")
                _uiState.update { state ->
                    state.copy(
                        connectionState = ConnectionState.Failed(it.message ?: "Socket closed"),
                        streamingState = StreamingState.Failed("Streaming stopped"),
                        error = ErrorState(ErrorReason.SOCKET_CLOSED, it.message ?: "Socket closed")
                    )
                }
            }
        }
    }

    override suspend fun startRtsp() {
        withContext(Dispatchers.IO) {
            runCatching { rtspServer.start() }
                .onSuccess { _uiState.update { it.copy(rtspEndpoint = rtspServer.endpoint()) } }
                .onFailure { _uiState.update { state -> state.copy(error = ErrorState(ErrorReason.NETWORK_LOST, it.message ?: "RTSP failed")) } }
        }
    }

    override suspend fun stopRtsp() {
        withContext(Dispatchers.IO) {
            rtspServer.stop()
            _uiState.update { it.copy(rtspEndpoint = "") }
        }
    }

    override fun reportError(error: ErrorState) {
        _uiState.update { it.copy(error = error) }
    }

    private fun failConnection(t: Throwable) {
        Timber.e(t, "Connection failed")
        _uiState.update {
            it.copy(
                connectionState = ConnectionState.Failed(t.message ?: "Connection failed"),
                error = ErrorState(ErrorReason.NETWORK_LOST, t.message ?: "Connection failed")
            )
        }
    }
}
