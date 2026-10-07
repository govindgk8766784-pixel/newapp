package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object BackgroundRemovalEngine {

    /**
     * Extracts foreground alpha mask using color difference and edge analysis.
     * Returns an alpha mask Bitmap where 255 = foreground, 0 = background.
     */
    fun generateCutoutMask(src: Bitmap, tolerance: Float = 0.22f): Bitmap {
        val w = src.width
        val h = src.height
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        // Sample background colors from top corners and edges (standard studio backdrop regions)
        val samplePoints = listOf(
            0, min(10, w - 1),
            w - 1, min(w - 11, w - 1),
            (h / 4) * w, (h / 4) * w + (w - 1),
            (h / 2) * w, (h / 2) * w + (w - 1)
        )

        var bgR = 0f
        var bgG = 0f
        var bgB = 0f
        var count = 0
        for (idx in samplePoints) {
            if (idx in pixels.indices) {
                val c = pixels[idx]
                bgR += Color.red(c)
                bgG += Color.green(c)
                bgB += Color.blue(c)
                count++
            }
        }
        if (count > 0) {
            bgR /= count
            bgG /= count
            bgB /= count
        }

        // Color distance threshold (0 to 441 max Euclidean distance in RGB)
        val threshold = tolerance * 441f
        val maskPixels = ByteArray(w * h)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)

            val dist = sqrt(((r - bgR) * (r - bgR) + (g - bgG) * (g - bgG) + (b - bgB) * (b - bgB)).toDouble()).toFloat()
            
            // Soft edge thresholding
            val alphaVal = when {
                dist < threshold * 0.75f -> 0
                dist > threshold * 1.25f -> 255
                else -> {
                    val factor = (dist - threshold * 0.75f) / (threshold * 0.5f)
                    (factor * 255f).toInt().coerceIn(0, 255)
                }
            }
            maskPixels[i] = alphaVal.toByte()
        }

        // Write to ALPHA_8 bitmap
        val buffer = java.nio.ByteBuffer.wrap(maskPixels)
        mask.copyPixelsFromBuffer(buffer)

        return mask
    }

    /**
     * Composites isolated foreground over chosen backdrop.
     * [bgType]: "transparent", "solid", "gradient"
     * [bgColor]: target hex / Int color for solid or gradient
     */
    fun compositeImage(
        src: Bitmap,
        mask: Bitmap,
        bgType: String = "solid",
        bgColor: Int = Color.WHITE,
        secondaryColor: Int = Color.parseColor("#E0F2FE")
    ): Bitmap {
        val w = src.width
        val h = src.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Draw Background
        when (bgType) {
            "transparent" -> {
                // Clear / transparent canvas
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            }
            "gradient" -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, 0f, h.toFloat(),
                        bgColor, secondaryColor,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            }
            else -> {
                // Solid color
                canvas.drawColor(bgColor)
            }
        }

        // 2. Draw masked foreground using PorterDuff DST_IN / SRC_ATOP
        val fgBitmap = src.copy(Bitmap.Config.ARGB_8888, true)
        val fgCanvas = Canvas(fgBitmap)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        fgCanvas.drawBitmap(mask, 0f, 0f, maskPaint)

        // 3. Composite over background
        canvas.drawBitmap(fgBitmap, 0f, 0f, null)
        fgBitmap.recycle()

        return output
    }

    /**
     * Applies manual touch-up brush stroke to the alpha mask.
     * [mode]: "erase" (turns alpha to 0) or "restore" (turns alpha to 255)
     */
    fun applyTouchUpStroke(
        mask: Bitmap,
        x: Float,
        y: Float,
        radius: Float,
        mode: String = "erase"
    ) {
        val canvas = Canvas(mask)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mode == "restore") Color.WHITE else Color.TRANSPARENT
            xfermode = if (mode == "restore") {
                PorterDuffXfermode(PorterDuff.Mode.SRC)
            } else {
                PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
        }
        canvas.drawCircle(x, y, radius, paint)
    }
}
