package com.maytic.objectdetection

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max

private val CategoryPalette = listOf(
    Color(0xFF2A78D6), // blue
    Color(0xFFEB6834), // orange
    Color(0xFF1BAF7A), // aqua
    Color(0xFFEDA100), // yellow
    Color(0xFFE87BA4), // magenta
    Color(0xFF008300), // green
    Color(0xFF4A3AA7), // violet
    Color(0xFFE34948), // red
)
private fun colorForCategory(categoryName: String): Color {
    val idx = (categoryName.hashCode() and 0x7fffffff) % CategoryPalette.size
    return CategoryPalette[idx]
}

@Composable
fun DetectionOverlay(
    detectionState: DetectionUISate?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val state = detectionState ?: return@Canvas

        val scaleFactor = max(size.width / state.imageWidth, size.height / state.imageHeight)
        val offsetX = (state.imageWidth * scaleFactor - size.width) / 2F
        val offsetY = (state.imageHeight * scaleFactor - size.height) / 2F


        state.result.detections().forEach { detection ->
            val box = detection.boundingBox()
            val left = box.left * scaleFactor - offsetX
            val top = box.top * scaleFactor - offsetY
            val right = box.right * scaleFactor - offsetX
            val bottom = box.bottom * scaleFactor - offsetY
            val category = detection.categories().firstOrNull()
            val boxColor = colorForCategory(category?.categoryName() ?: "unknown")


            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4F)
            )

            val label = category?.let { "${it.categoryName()} ${(it.score() * 100).toInt()}%" } ?: ""

            val textPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 36f
                isAntiAlias = true
            }
            val textHeight = textPaint.descent() - textPaint.ascent()
            val textWidth = textPaint.measureText(label)
            val labelTop = (top - textHeight - 4f).coerceAtLeast(0f)

            drawContext.canvas.nativeCanvas.apply {
                drawRect(
                    left, labelTop, left + textWidth + 8f, labelTop + textHeight + 4f,
                    Paint().apply { color = boxColor.toArgb() }
                )
                drawText(label, left + 4f, labelTop + textHeight - textPaint.descent(), textPaint)
            }
        }

    }
}