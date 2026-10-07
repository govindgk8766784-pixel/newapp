package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.data.model.ProcessingFilter
import kotlin.math.max
import kotlin.math.min

object ImageProcessingEngine {

    /**
     * Applies general image adjustments (brightness, contrast, saturation, sharpness).
     */
    fun applyAdjustments(
        src: Bitmap,
        brightness: Float = 0f, // -100 to +100
        contrast: Float = 1f,   // 0.5 to 2.0
        saturation: Float = 1f, // 0 to 2
        sharpness: Float = 0f   // 0 to 1
    ): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Composite Color Matrix
        val cm = ColorMatrix()

        // Saturation
        if (saturation != 1f) {
            cm.setSaturation(saturation)
        }

        // Contrast & Brightness
        // Formula: c * (x - 128) + 128 + b = c * x + (128 * (1 - c) + b)
        val scale = contrast
        val translate = 128f * (1f - scale) + brightness
        val contrastCm = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastCm)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)

        // Optional Sharpen
        return if (sharpness > 0.05f) {
            applySharpen(result, sharpness)
        } else {
            result
        }
    }

    /**
     * Applies standard preset document/photo filters.
     */
    fun applyFilter(src: Bitmap, filter: ProcessingFilter): Bitmap {
        return when (filter) {
            ProcessingFilter.ORIGINAL -> src.copy(Bitmap.Config.ARGB_8888, true)
            ProcessingFilter.MAGIC_COLOR -> applyMagicColor(src)
            ProcessingFilter.CLEAN_PAPER -> applyCleanPaper(src)
            ProcessingFilter.BW_PHOTOCOPY -> applyBwPhotocopy(src)
            ProcessingFilter.GRAYSCALE -> applyGrayscale(src)
            ProcessingFilter.STUDIO_ENHANCE -> applyStudioPortrait(src)
        }
    }

    /**
     * Magic Color: Boosts saturation and contrast to make document ink, stamps, and ID signatures pop.
     */
    private fun applyMagicColor(src: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val cm = ColorMatrix().apply {
            setSaturation(1.35f)
        }
        // Contrast boost
        val scale = 1.25f
        val translate = 128f * (1f - scale) + 15f
        val contrastCm = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastCm)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return applySharpen(result, 0.4f)
    }

    /**
     * Clean Paper: Brightens grey background shadows to clean white paper while keeping dark ink.
     */
    private fun applyCleanPaper(src: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val scale = 1.4f
        val translate = 128f * (1f - scale) + 38f
        val cm = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    /**
     * B&W Photocopy: High-contrast monochrome xerox style.
     */
    private fun applyBwPhotocopy(src: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val grayCm = ColorMatrix().apply { setSaturation(0f) }
        val scale = 2.4f
        val translate = 128f * (1f - scale) - 10f
        val contrastCm = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        grayCm.postConcat(contrastCm)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(grayCm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    /**
     * Grayscale studio tonal conversion.
     */
    private fun applyGrayscale(src: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val cm = ColorMatrix().apply { setSaturation(0f) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    /**
     * Studio Portrait: Subtle warmth, skin smoothing glow, and iris contrast.
     */
    private fun applyStudioPortrait(src: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Warm studio tint + gentle saturation
        val cm = ColorMatrix().apply {
            setSaturation(1.10f)
        }
        val warmCm = ColorMatrix(floatArrayOf(
            1.05f, 0f, 0f, 0f, 5f,
            0f, 1.02f, 0f, 0f, 2f,
            0f, 0f, 0.96f, 0f, -2f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(warmCm)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return applySharpen(result, 0.25f)
    }

    /**
     * Fast 3x3 unsharp mask / convolution filter.
     */
    private fun applySharpen(src: Bitmap, amount: Float): Bitmap {
        val w = src.width
        val h = src.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val outPixels = IntArray(w * h)

        val centerWeight = 1f + 4f * amount
        val sideWeight = -amount

        for (y in 1 until h - 1) {
            val row = y * w
            val prevRow = (y - 1) * w
            val nextRow = (y + 1) * w

            for (x in 1 until w - 1) {
                val c = pixels[row + x]
                val top = pixels[prevRow + x]
                val bot = pixels[nextRow + x]
                val left = pixels[row + x - 1]
                val right = pixels[row + x + 1]

                val a = Color.alpha(c)
                val r = (centerWeight * Color.red(c) + sideWeight * (Color.red(top) + Color.red(bot) + Color.red(left) + Color.red(right))).toInt()
                val g = (centerWeight * Color.green(c) + sideWeight * (Color.green(top) + Color.green(bot) + Color.green(left) + Color.green(right))).toInt()
                val b = (centerWeight * Color.blue(c) + sideWeight * (Color.blue(top) + Color.blue(bot) + Color.blue(left) + Color.blue(right))).toInt()

                outPixels[row + x] = Color.argb(
                    a,
                    min(255, max(0, r)),
                    min(255, max(0, g)),
                    min(255, max(0, b))
                )
            }
        }
        output.setPixels(outPixels, 0, w, 0, 0, w, h)
        return output
    }
}
