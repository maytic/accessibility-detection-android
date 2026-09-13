package com.maytic.objectdetection

import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

data class DetectionUISate(
    val result: ObjectDetectorResult,
    val imageWidth: Int,
    val imageHeight: Int,
)
