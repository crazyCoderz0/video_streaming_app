package com.example.streamer.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.streamer.R
import com.example.streamer.camera.CameraController
import com.example.streamer.databinding.ActivityMainBinding
import com.example.streamer.domain.ConnectionState
import com.example.streamer.domain.StreamingState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var cameraController: CameraController

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val cameraGranted = result[Manifest.permission.CAMERA] == true
        if (cameraGranted) bindCamera() else viewModel.permissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.portInput.setText("9000")
        binding.connectButton.setOnClickListener {
            val connected = viewModel.uiState.value.connectionState is ConnectionState.Connected
            if (connected) viewModel.disconnect() else viewModel.connect(binding.ipInput.text.toString(), binding.portInput.text.toString())
        }
        binding.streamButton.setOnClickListener {
            if (viewModel.uiState.value.streamingState == StreamingState.Streaming) viewModel.stopStream() else viewModel.startStream()
        }
        collectState()
        requestPermissions()
    }

    override fun onStop() {
        viewModel.stopStream()
        super.onStop()
    }

    override fun onDestroy() {
        cameraController.release()
        super.onDestroy()
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val connected = state.connectionState is ConnectionState.Connected
                    binding.connectButton.text = getString(if (connected) R.string.disconnect else R.string.connect)
                    binding.streamButton.text = getString(if (state.streamingState == StreamingState.Streaming) R.string.stop_stream else R.string.start_stream)
                    binding.statusText.setTextColor(ContextCompat.getColor(this@MainActivity, if (connected) R.color.connected else R.color.disconnected))
                    binding.statusText.text = buildString {
                        append(getString(if (connected) R.string.status_connected else R.string.status_disconnected))
                        if (state.rtspEndpoint.isNotBlank()) append(" | ").append(getString(R.string.status_rtsp, state.rtspEndpoint))
                        state.error?.let { append(" | ").append(it.message) }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_title)
                .setMessage(R.string.permission_message)
                .setPositiveButton(android.R.string.ok) { _, _ -> permissionLauncher.launch(permissions) }
                .setNegativeButton(android.R.string.cancel) { _, _ -> viewModel.permissionDenied() }
                .show()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun bindCamera() {
        cameraController.bind(this, this, binding.previewView)
    }
}
