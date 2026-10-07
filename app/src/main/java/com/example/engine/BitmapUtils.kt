package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object BitmapUtils {

    /**
     * Safely loads a bitmap from a content Uri, handling EXIF orientation and downsampling.
     */
    fun decodeUriWithExif(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        return try {
            // Step 1: Decode bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var stream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream, null, options)
            stream?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            // Step 2: Compute sample size
            var sampleSize = 1
            var w = origWidth
            var h = origHeight
            while (w > maxDimension || h > maxDimension) {
                w /= 2
                h /= 2
                sampleSize *= 2
            }

            // Step 3: Decode bitmap
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            stream = context.contentResolver.openInputStream(uri)
            val decoded = BitmapFactory.decodeStream(stream, null, options)
            stream?.close()
            if (decoded == null) return null

            // Step 4: Fix EXIF rotation
            val rotationDegrees = getExifRotation(context, uri)
            if (rotationDegrees != 0) {
                rotateBitmap(decoded, rotationDegrees.toFloat())
            } else {
                decoded
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getExifRotation(context: Context, uri: Uri): Int {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return 0
            val exif = ExifInterface(stream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            stream.close()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    /**
     * Crops bitmap to specified aspect ratio centered on face/content.
     */
    fun cropToAspectRatio(bitmap: Bitmap, targetRatio: Float): Bitmap {
        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val (cropW, cropH) = if (currentRatio > targetRatio) {
            // Too wide -> crop width
            val newW = (bitmap.height * targetRatio).toInt()
            Pair(newW, bitmap.height)
        } else {
            // Too tall -> crop height
            val newH = (bitmap.width / targetRatio).toInt()
            Pair(bitmap.width, newH)
        }
        val startX = (bitmap.width - cropW) / 2
        val startY = (bitmap.height - cropH) / 2
        return Bitmap.createBitmap(bitmap, max(0, startX), max(0, startY), cropW, cropH)
    }

    /**
     * Saves bitmap to internal app storage (cache/files) and returns the absolute path.
     */
    fun saveToInternalStorage(context: Context, bitmap: Bitmap, filename: String, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 95): File {
        val dir = File(context.filesDir, "studio_exports").apply { if (!exists()) mkdirs() }
        val file = File(dir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(format, quality, out)
        }
        return file
    }

    /**
     * Exports bitmap directly to user's public Pictures / DCIM folder.
     */
    fun exportToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$title.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PixelPro")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 98, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            return uri
        } catch (e: Exception) {
            e.printStackTrace()
            resolver.delete(uri, null, null)
            return null
        }
    }

    /**
     * Share image via Android Share Sheet.
     */
    fun shareFile(context: Context, file: File, mimeType: String = "image/jpeg", title: String = "Share PixelPro Export") {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Generates a realistic high quality studio portrait test pattern for immediate preview.
     */
    fun createSampleStudioPortrait(width: Int = 600, height: Int = 800): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        
        // Studio backdrop gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2EEFB") // soft studio blue
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Studio lighting radial vignette
        val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFFF")
            alpha = 140
        }
        canvas.drawCircle(width * 0.5f, height * 0.35f, width * 0.45f, lightPaint)

        // Silhouette - Shoulders
        val suitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B") // Dark navy formal suit
        }
        val suitPath = android.graphics.Path().apply {
            moveTo(width * 0.12f, height.toFloat())
            quadTo(width * 0.28f, height * 0.65f, width * 0.38f, height * 0.60f)
            lineTo(width * 0.50f, height * 0.72f) // collar V
            lineTo(width * 0.62f, height * 0.60f)
            quadTo(width * 0.72f, height * 0.65f, width * 0.88f, height.toFloat())
            close()
        }
        canvas.drawPath(suitPath, suitPaint)

        // White formal collar & tie
        val shirtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val shirtPath = android.graphics.Path().apply {
            moveTo(width * 0.40f, height * 0.58f)
            lineTo(width * 0.50f, height * 0.70f)
            lineTo(width * 0.60f, height * 0.58f)
            close()
        }
        canvas.drawPath(shirtPath, shirtPaint)

        val tiePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#991B1B") }
        val tiePath = android.graphics.Path().apply {
            moveTo(width * 0.47f, height * 0.62f)
            lineTo(width * 0.53f, height * 0.62f)
            lineTo(width * 0.51f, height * 0.85f)
            lineTo(width * 0.49f, height * 0.85f)
            close()
        }
        canvas.drawPath(tiePath, tiePaint)

        // Neck
        val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0AC69") }
        canvas.drawRect(width * 0.42f, height * 0.48f, width * 0.58f, height * 0.60f, skinPaint)

        // Head oval
        canvas.drawOval(RectF(width * 0.32f, height * 0.22f, width * 0.68f, height * 0.55f), skinPaint)

        // Hair
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1C1917") }
        canvas.drawOval(RectF(width * 0.30f, height * 0.18f, width * 0.70f, height * 0.35f), hairPaint)

        // Subtle studio watermark/test badge
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PIXELPRO STUDIO DEMO", width * 0.5f, height * 0.95f, textPaint)

        return bmp
    }
}
