package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Official biometric head alignment guide overlay for passport photos.
 * Shows chin position, eye line, and crown of head limits.
 */
@Composable
fun BiometricGuideOverlay(
    modifier: Modifier = Modifier,
    showGuides: Boolean = true
) {
    if (!showGuides) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Head oval boundary (typically 70% of photo height)
        val ovalW = w * 0.52f
        val ovalH = h * 0.62f
        val ovalLeft = (w - ovalW) / 2f
        val ovalTop = h * 0.14f

        val guideColor = Color(0xFF38BDF8)
        val guideDashed = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

        // Draw Outer Head Oval
        drawOval(
            color = guideColor.copy(alpha = 0.85f),
            topLeft = Offset(ovalLeft, ovalTop),
            size = Size(ovalW, ovalH),
            style = Stroke(width = 3f, pathEffect = guideDashed)
        )

        // Eye Line Guide (approx 55% from top of head)
        val eyeY = ovalTop + ovalH * 0.42f
        drawLine(
            color = Color(0xFFFBBF24).copy(alpha = 0.85f),
            start = Offset(ovalLeft * 0.8f, eyeY),
            end = Offset(w - ovalLeft * 0.8f, eyeY),
            strokeWidth = 2f,
            pathEffect = guideDashed
        )

        // Chin Line Guide
        val chinY = ovalTop + ovalH
        drawLine(
            color = Color(0xFF34D399).copy(alpha = 0.85f),
            start = Offset(w * 0.25f, chinY),
            end = Offset(w * 0.75f, chinY),
            strokeWidth = 2.5f
        )

        // Vertical Center Line
        drawLine(
            color = guideColor.copy(alpha = 0.4f),
            start = Offset(w * 0.5f, h * 0.08f),
            end = Offset(w * 0.5f, h * 0.92f),
            strokeWidth = 1.5f,
            pathEffect = guideDashed
        )
    }
}
