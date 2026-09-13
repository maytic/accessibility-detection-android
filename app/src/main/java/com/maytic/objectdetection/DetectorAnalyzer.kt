package com.maytic.objectdetection

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector

class DetectorAnalyzer (
    private val objectDetector: ObjectDetector,
): ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val bitmapBuffer = image.toBitmap()
        val rotationDegrees = image.imageInfo.rotationDegrees

        image.close()

        val rotatedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
        } else {
            bitmapBuffer
        }

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        objectDetector.detectAsync(mpImage, SystemClock.uptimeMillis())
    }
}