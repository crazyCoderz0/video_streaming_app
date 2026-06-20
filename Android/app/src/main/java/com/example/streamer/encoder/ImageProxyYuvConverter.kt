package com.example.streamer.encoder

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

object ImageProxyYuvConverter {
    fun writeI420(image: ImageProxy, output: ByteBuffer, targetWidth: Int, targetHeight: Int) {
        require(image.format == android.graphics.ImageFormat.YUV_420_888) { "Unsupported image format" }
        val crop = image.cropRect
        val planes = image.planes
        copyPlane(planes[0], crop.left, crop.top, crop.width(), crop.height(), output, 1)
        copyPlane(planes[1], crop.left / 2, crop.top / 2, crop.width() / 2, crop.height() / 2, output, 2)
        copyPlane(planes[2], crop.left / 2, crop.top / 2, crop.width() / 2, crop.height() / 2, output, 2)
        val expected = targetWidth * targetHeight * 3 / 2
        if (output.position() > expected) output.position(expected)
    }

    private fun copyPlane(
        plane: ImageProxy.PlaneProxy,
        cropX: Int,
        cropY: Int,
        width: Int,
        height: Int,
        output: ByteBuffer,
        chromaScale: Int
    ) {
        val buffer = plane.buffer.duplicate()
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val row = ByteArray(rowStride)
        repeat(height) { y ->
            val offset = (y + cropY) * rowStride + cropX * pixelStride
            buffer.position(offset.coerceAtMost(buffer.limit()))
            val bytesToRead = minOf(rowStride, buffer.remaining())
            buffer.get(row, 0, bytesToRead)
            var x = 0
            while (x < width) {
                val index = x * pixelStride
                if (index < bytesToRead) output.put(row[index])
                x += chromaScale / chromaScale
            }
        }
    }
}

