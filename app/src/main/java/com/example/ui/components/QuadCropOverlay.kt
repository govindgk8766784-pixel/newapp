package com.example.ui.components

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.engine.QuadCorners
import kotlin.math.hypot

@Composable
fun QuadCropOverlay(
    corners: QuadCorners,
    onCornersChanged: (QuadCorners) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeHandle by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(corners) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val touchRadius = 60f
                        val dTL = hypot(offset.x - corners.topLeft.x, offset.y - corners.topLeft.y)
                        val dTR = hypot(offset.x - corners.topRight.x, offset.y - corners.topRight.y)
                        val dBR = hypot(offset.x - corners.bottomRight.x, offset.y - corners.bottomRight.y)
                        val dBL = hypot(offset.x - corners.bottomLeft.x, offset.y - corners.bottomLeft.y)

                        val minD = minOf(dTL, dTR, dBR, dBL)
                        if (minD <= touchRadius) {
                            activeHandle = when (minD) {
                                dTL -> "TL"
                                dTR -> "TR"
                                dBR -> "BR"
                                else -> "BL"
                            }
                        }
                    },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val currentHandle = activeHandle ?: return@detectDragGestures

                        val newCorners = when (currentHandle) {
                            "TL" -> corners.copy(
                                topLeft = PointF(
                                    corners.topLeft.x + dragAmount.x,
                                    corners.topLeft.y + dragAmount.y
                                )
                            )
                            "TR" -> corners.copy(
                                topRight = PointF(
                                    corners.topRight.x + dragAmount.x,
                                    corners.topRight.y + dragAmount.y
                                )
                            )
                            "BR" -> corners.copy(
                                bottomRight = PointF(
                                    corners.bottomRight.x + dragAmount.x,
                                    corners.bottomRight.y + dragAmount.y
                                )
                            )
                            "BL" -> corners.copy(
                                bottomLeft = PointF(
                                    corners.bottomLeft.x + dragAmount.x,
                                    corners.bottomLeft.y + dragAmount.y
                                )
                            )
                            else -> corners
                        }
                        onCornersChanged(newCorners)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = Color(0xFF38BDF8)
            val fillColor = Color(0xFF0EA5E9).copy(alpha = 0.18f)

            // Draw quadrilateral polygon
            val quadPath = Path().apply {
                moveTo(corners.topLeft.x, corners.topLeft.y)
                lineTo(corners.topRight.x, corners.topRight.y)
                lineTo(corners.bottomRight.x, corners.bottomRight.y)
                lineTo(corners.bottomLeft.x, corners.bottomLeft.y)
                close()
            }
            drawPath(quadPath, color = fillColor)
            drawPath(quadPath, color = strokeColor, style = Stroke(width = 4f))

            // Draw corner handles
            val handleRadius = 24f
            val handles = listOf(
                Pair(corners.topLeft, "TL"),
                Pair(corners.topRight, "TR"),
                Pair(corners.bottomRight, "BR"),
                Pair(corners.bottomLeft, "BL")
            )

            for ((pt, tag) in handles) {
                val isActive = tag == activeHandle
                val handleColor = if (isActive) Color(0xFFF59E0B) else Color(0xFF38BDF8)

                // Outer circle
                drawCircle(
                    color = handleColor,
                    radius = if (isActive) handleRadius * 1.3f else handleRadius,
                    center = Offset(pt.x, pt.y)
                )
                // Inner white dot
                drawCircle(
                    color = Color.White,
                    radius = handleRadius * 0.45f,
                    center = Offset(pt.x, pt.y)
                )
            }
        }
    }
}
