package com.example.streamer.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import androidx.camera.core.ImageProxy
import com.example.streamer.domain.EncodedFrame
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class H264Encoder(
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val bitRate: Int
) {
    private var codec: MediaCodec? = null
    private val started = AtomicBoolean(false)
    private val _frames = MutableSharedFlow<EncodedFrame>(extraBufferCapacity = 8)
    val frames: SharedFlow<EncodedFrame> = _frames

    fun start() {
        if (started.getAndSet(true)) return
        val format = MediaFormat.createVideoFormat(MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setInteger(MediaFormat.KEY_LATENCY, 0)
        }
        codec = MediaCodec.createEncoderByType(MIME).also {
            it.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            it.start()
        }
    }

    fun encode(image: ImageProxy) {
        val activeCodec = codec ?: return image.close()
        if (!started.get()) return image.close()
        try {
            val inputIndex = activeCodec.dequeueInputBuffer(0)
            if (inputIndex >= 0) {
                val input = activeCodec.getInputBuffer(inputIndex) ?: return
                input.clear()
                ImageProxyYuvConverter.writeI420(image, input, width, height)
                activeCodec.queueInputBuffer(inputIndex, 0, input.position(), image.imageInfo.timestamp / 1000, 0)
            }
            drain(activeCodec)
        } catch (t: Throwable) {
            Timber.e(t, "H264 encode failed")
        } finally {
            image.close()
        }
    }

    fun stop() {
        if (!started.getAndSet(false)) return
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null
    }

    private fun drain(activeCodec: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        while (true) {
            val outputIndex = activeCodec.dequeueOutputBuffer(info, 0)
            if (outputIndex < 0) break
            val output = activeCodec.getOutputBuffer(outputIndex)
            if (output != null && info.size > 0) {
                output.position(info.offset)
                output.limit(info.offset + info.size)
                val bytes = ByteArray(info.size)
                output.get(bytes)
                val flags = info.flags
                _frames.tryEmit(
                    EncodedFrame(
                        bytes = bytes,
                        timestampUs = info.presentationTimeUs,
                        isKeyFrame = flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0,
                        isConfig = flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    )
                )
            }
            activeCodec.releaseOutputBuffer(outputIndex, false)
        }
    }

    companion object {
        private const val MIME = "video/avc"
    }
}

