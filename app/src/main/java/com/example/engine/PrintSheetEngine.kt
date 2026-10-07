package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.model.PaperSize
import com.example.data.model.PrintSheetConfig
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object PrintSheetEngine {

    /**
     * Renders a photo grid print sheet bitmap according to configuration.
     */
    fun generatePrintSheetBitmap(
        photoBitmap: Bitmap,
        config: PrintSheetConfig
    ): Bitmap {
        val sheetWidthPx = config.paperSize.getPixelWidth(config.dpi)
        val sheetHeightPx = config.paperSize.getPixelHeight(config.dpi)

        val sheetBitmap = Bitmap.createBitmap(sheetWidthPx, sheetHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        // White paper background
        canvas.drawColor(Color.WHITE)

        val pxPerMm = (config.dpi / 25.4f)
        val marginPx = config.marginMm * pxPerMm
        val gapPx = config.gapMm * pxPerMm

        val availableW = sheetWidthPx - 2 * marginPx
        val availableH = sheetHeightPx - 2 * marginPx

        // Determine columns and rows based on target count and paper orientation
        val (cols, rows) = calculateOptimalGrid(config.photoCount, availableW, availableH, photoBitmap.width.toFloat() / photoBitmap.height.toFloat())

        val cellW = (availableW - (cols - 1) * gapPx) / cols
        val cellH = (availableH - (rows - 1) * gapPx) / rows

        // Compute aspect-fit dimensions for each photo inside the cell
        val photoRatio = photoBitmap.width.toFloat() / photoBitmap.height.toFloat()
        val cellRatio = cellW / cellH

        val (drawW, drawH) = if (photoRatio > cellRatio) {
            Pair(cellW, cellW / photoRatio)
        } else {
            Pair(cellH * photoRatio, cellH)
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8") // Slate hairline border
            style = Paint.Style.STROKE
            strokeWidth = max(1f, pxPerMm * 0.2f)
        }

        val cutGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = max(1f, pxPerMm * 0.15f)
            pathEffect = DashPathEffect(floatArrayOf(pxPerMm * 2f, pxPerMm * 1.5f), 0f)
        }

        val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            style = Paint.Style.STROKE
            strokeWidth = max(2f, pxPerMm * 0.35f)
        }

        var renderedCount = 0
        val markLength = pxPerMm * 3.5f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (renderedCount >= config.photoCount) break

                val cellLeft = marginPx + c * (cellW + gapPx)
                val cellTop = marginPx + r * (cellH + gapPx)

                val photoLeft = cellLeft + (cellW - drawW) / 2f
                val photoTop = cellTop + (cellH - drawH) / 2f
                val photoRect = RectF(photoLeft, photoTop, photoLeft + drawW, photoTop + drawH)

                // Draw photo
                canvas.drawBitmap(photoBitmap, null, photoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

                // Cut border around each individual photo
                if (config.showCutBorder) {
                    canvas.drawRect(photoRect, borderPaint)
                }

                // Corner registration marks
                if (config.showCornerMarks) {
                    // Top-left
                    canvas.drawLine(photoLeft - markLength, photoTop, photoLeft, photoTop, markPaint)
                    canvas.drawLine(photoLeft, photoTop - markLength, photoLeft, photoTop, markPaint)
                    // Top-right
                    canvas.drawLine(photoRect.right, photoTop, photoRect.right + markLength, photoTop, markPaint)
                    canvas.drawLine(photoRect.right, photoTop - markLength, photoRect.right, photoTop, markPaint)
                    // Bottom-left
                    canvas.drawLine(photoLeft - markLength, photoRect.bottom, photoLeft, photoRect.bottom, markPaint)
                    canvas.drawLine(photoLeft, photoRect.bottom, photoLeft, photoRect.bottom + markLength, markPaint)
                    // Bottom-right
                    canvas.drawLine(photoRect.right, photoRect.bottom, photoRect.right + markLength, photoRect.bottom, markPaint)
                    canvas.drawLine(photoRect.right, photoRect.bottom, photoRect.right, photoRect.bottom + markLength, markPaint)
                }

                renderedCount++
            }
        }

        return sheetBitmap
    }

    /**
     * Generates a printable PDF file with exact 300 DPI page dimensions.
     */
    fun exportToPdf(
        context: Context,
        sheetBitmap: Bitmap,
        filename: String = "pixelpro_print_sheet.pdf"
    ): File {
        val pdfDocument = PdfDocument()

        // Page sizes in PostScript points (1/72 inch). 1 pt = 1/72 in.
        // A4 is 595 x 842 pt. 4x6 in is 288 x 432 pt.
        val ptWidth = (sheetBitmap.width * 72f / 300f).toInt()
        val ptHeight = (sheetBitmap.height * 72f / 300f).toInt()

        val pageInfo = PdfDocument.PageInfo.Builder(ptWidth, ptHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val dstRect = Rect(0, 0, ptWidth, ptHeight)
        canvas.drawBitmap(sheetBitmap, null, dstRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        pdfDocument.finishPage(page)

        val dir = File(context.filesDir, "studio_exports").apply { if (!exists()) mkdirs() }
        val pdfFile = File(dir, filename)
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    private fun calculateOptimalGrid(count: Int, sheetW: Float, sheetH: Float, photoRatio: Float): Pair<Int, Int> {
        return when (count) {
            2 -> if (sheetW > sheetH) Pair(2, 1) else Pair(1, 2)
            4 -> Pair(2, 2)
            6 -> if (sheetW > sheetH) Pair(3, 2) else Pair(2, 3)
            8 -> if (sheetW > sheetH) Pair(4, 2) else Pair(2, 4)
            12 -> if (sheetW > sheetH) Pair(4, 3) else Pair(3, 4)
            16 -> Pair(4, 4)
            24 -> if (sheetW > sheetH) Pair(6, 4) else Pair(4, 6)
            32 -> if (sheetW > sheetH) Pair(8, 4) else Pair(4, 8)
            else -> {
                val cols = ceil(sqrt(count.toDouble())).toInt()
                val rows = ceil(count.toDouble() / cols).toInt()
                Pair(cols, rows)
            }
        }
    }
}
