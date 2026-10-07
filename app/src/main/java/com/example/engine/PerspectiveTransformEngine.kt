package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.hypot
import kotlin.math.max

data class QuadCorners(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
) {
    companion object {
        fun defaultFor(width: Float, height: Float, marginFraction: Float = 0.08f): QuadCorners {
            val mx = width * marginFraction
            val my = height * marginFraction
            return QuadCorners(
                topLeft = PointF(mx, my),
                topRight = PointF(width - mx, my),
                bottomRight = PointF(width - mx, height - my),
                bottomLeft = PointF(mx, height - my)
            )
        }
    }
}

object PerspectiveTransformEngine {

    /**
     * Warps the quadrilateral defined by the 4 corners into an orthogonal rectangular document.
     */
    fun warpQuadToRectangle(src: Bitmap, corners: QuadCorners): Bitmap {
        // Calculate estimated output width and height based on the maximum side lengths
        val topWidth = hypot((corners.topRight.x - corners.topLeft.x).toDouble(), (corners.topRight.y - corners.topLeft.y).toDouble())
        val bottomWidth = hypot((corners.bottomRight.x - corners.bottomLeft.x).toDouble(), (corners.bottomRight.y - corners.bottomLeft.y).toDouble())
        val outWidth = max(topWidth, bottomWidth).toFloat().coerceAtLeast(100f)

        val leftHeight = hypot((corners.bottomLeft.x - corners.topLeft.x).toDouble(), (corners.bottomLeft.y - corners.topLeft.y).toDouble())
        val rightHeight = hypot((corners.bottomRight.x - corners.topRight.x).toDouble(), (corners.bottomRight.y - corners.topRight.y).toDouble())
        val outHeight = max(leftHeight, rightHeight).toFloat().coerceAtLeast(100f)

        val outW = outWidth.toInt().coerceAtMost(4096)
        val outH = outHeight.toInt().coerceAtMost(4096)

        val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Source quad points: TL, TR, BR, BL
        val srcPoints = floatArrayOf(
            corners.topLeft.x, corners.topLeft.y,
            corners.topRight.x, corners.topRight.y,
            corners.bottomRight.x, corners.bottomRight.y,
            corners.bottomLeft.x, corners.bottomLeft.y
        )

        // Destination rectangle points: TL, TR, BR, BL
        val dstPoints = floatArrayOf(
            0f, 0f,
            outW.toFloat(), 0f,
            outW.toFloat(), outH.toFloat(),
            0f, outH.toFloat()
        )

        // Matrix mapping dst rect back to src quad for backward mapping, OR src quad to dst rect!
        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        if (success) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(src, matrix, paint)
        } else {
            // Fallback: simple copy
            canvas.drawBitmap(src, 0f, 0f, null)
        }

        return output
    }
}
