package com.semid.qrcodescanner

import android.annotation.SuppressLint
import android.graphics.*
import android.hardware.Camera
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

 object Utils {
    fun ImageProxy.toBitmap(): Bitmap? {
        val yBuffer = planes[0].buffer // Y
        val vuBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

     fun Bitmap.applyNegativeEffect(): Bitmap {
        val width = this.width
        val height = this.height

        // Create a mutable bitmap to store the negative image
        val negativeBitmap = Bitmap.createBitmap(width, height, config)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = this.getPixel(x, y)
                val invertedPixel = Color.rgb(
                    255 - Color.red(pixel),
                    255 - Color.green(pixel),
                    255 - Color.blue(pixel)
                )
                negativeBitmap.setPixel(x, y, invertedPixel)
            }
        }

        return negativeBitmap
    }
}