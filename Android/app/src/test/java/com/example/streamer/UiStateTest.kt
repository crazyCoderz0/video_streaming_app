package com.example.streamer

import com.example.streamer.domain.ConnectionState
import com.example.streamer.domain.StreamingState
import com.example.streamer.domain.UiState
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateTest {
    @Test
    fun defaultsAreDisconnectedAndStopped() {
        val state = UiState()
        assertTrue(state.connectionState is ConnectionState.Disconnected)
        assertTrue(state.streamingState is StreamingState.Stopped)
    }
}

