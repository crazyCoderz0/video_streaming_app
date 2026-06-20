package com.example.streamer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamer.domain.ErrorReason
import com.example.streamer.domain.ErrorState
import com.example.streamer.domain.StreamRepository
import com.example.streamer.domain.UiState
import com.example.streamer.encoder.H264Encoder
import com.example.streamer.utils.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: StreamRepository,
    private val encoder: H264Encoder
) : ViewModel() {
    val uiState: StateFlow<UiState> = repository.uiState

    init {
        viewModelScope.launch {
            encoder.frames.collect { repository.sendFrame(it) }
        }
        viewModelScope.launch { repository.startRtsp() }
    }

    fun connect(host: String, port: String) {
        val target = InputValidator.parseTarget(host, port).getOrElse {
            setValidationError(it.message ?: "Invalid host or port")
            return
        }
        viewModelScope.launch { repository.connect(target) }
    }

    fun disconnect() = viewModelScope.launch { repository.disconnect() }

    fun startStream() {
        encoder.start()
        viewModelScope.launch { repository.startStreaming() }
    }

    fun stopStream() {
        encoder.stop()
        viewModelScope.launch { repository.stopStreaming() }
    }

    fun permissionDenied() {
        setValidationError("Camera permission denied")
    }

    private fun setValidationError(message: String) {
        repository.reportError(ErrorState(ErrorReason.VALIDATION, message))
        viewModelScope.launch { repository.stopStreaming() }
    }

    override fun onCleared() {
        encoder.stop()
        viewModelScope.launch {
            repository.stopRtsp()
            repository.disconnect()
        }
        super.onCleared()
    }
}
