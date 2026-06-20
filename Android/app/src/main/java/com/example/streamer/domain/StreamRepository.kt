package com.example.streamer.domain

import kotlinx.coroutines.flow.StateFlow

interface StreamRepository {
    val uiState: StateFlow<UiState>
    suspend fun connect(target: StreamTarget)
    suspend fun disconnect()
    suspend fun startStreaming()
    suspend fun stopStreaming()
    suspend fun sendFrame(frame: EncodedFrame)
    suspend fun startRtsp()
    suspend fun stopRtsp()
    fun reportError(error: ErrorState)
}
