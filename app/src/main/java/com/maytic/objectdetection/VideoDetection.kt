package com.maytic.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import kotlinx.coroutines.delay

private const val SAMPLE_INTERVAL_MS = 200L

/**
 * Samples the currently-playing position (reported by [positionMsProvider], e.g.
 * an ExoPlayer's currentPosition) rather than stepping through the video itself,
 * so playback stays smooth while detection runs at its own, slower pace.
 */
suspend fun sampleVideoDetections(
    context: Context,
    assetFileName: String,
    positionMsProvider: () -> Long,
    objectDetector: ObjectDetector,
    onFrame: (Bitmap, DetectionUISate) -> Unit,
) {
    val retriever = MediaMetadataRetriever()
    try {
        context.assets.openFd(assetFileName).use { afd ->
            retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        }

        while (true) {
            val positionMs = positionMsProvider()
            val bitmap = retriever.getFrameAtTime(
                positionMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST
            )
            if (bitmap != null) {
                val mpImage = BitmapImageBuilder(bitmap).build()
                // SystemClock.uptimeMillis() (not positionMs) since the video loops
                // and detectForVideo requires strictly increasing timestamps.
                val result = objectDetector.detectForVideo(mpImage, SystemClock.uptimeMillis())
                onFrame(bitmap, DetectionUISate(result, bitmap.width, bitmap.height))
            }
            delay(SAMPLE_INTERVAL_MS)
        }
    } finally {
        retriever.release()
    }
}
