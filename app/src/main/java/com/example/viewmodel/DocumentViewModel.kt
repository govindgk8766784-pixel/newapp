package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PixelProDatabase
import com.example.data.model.ProcessingFilter
import com.example.data.model.StudioProject
import com.example.data.repository.StudioProjectRepository
import com.example.engine.BitmapUtils
import com.example.engine.ImageProcessingEngine
import com.example.engine.PerspectiveTransformEngine
import com.example.engine.PrintSheetEngine
import com.example.engine.QuadCorners
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PixelProDatabase.getDatabase(application)
    private val repository = StudioProjectRepository(db.studioProjectDao())

    private val _sourceBitmap = MutableStateFlow<Bitmap?>(null)
    val sourceBitmap: StateFlow<Bitmap?> = _sourceBitmap.asStateFlow()

    private val _corners = MutableStateFlow<QuadCorners?>(null)
    val corners: StateFlow<QuadCorners?> = _corners.asStateFlow()

    private val _warpedBitmap = MutableStateFlow<Bitmap?>(null)
    val warpedBitmap: StateFlow<Bitmap?> = _warpedBitmap.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ProcessingFilter.CLEAN_PAPER)
    val selectedFilter: StateFlow<ProcessingFilter> = _selectedFilter.asStateFlow()

    private val _isCroppingMode = MutableStateFlow(true)
    val isCroppingMode: StateFlow<Boolean> = _isCroppingMode.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        loadSampleDocument()
    }

    fun loadSampleDocument() {
        viewModelScope.launch(Dispatchers.Default) {
            val sample = createSampleTiltedDocument()
            _sourceBitmap.value = sample
            initCorners(sample.width.toFloat(), sample.height.toFloat())
            warpDocument()
        }
    }

    fun setSourceUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val bmp = BitmapUtils.decodeUriWithExif(getApplication(), uri)
            _sourceBitmap.value = bmp
            _isProcessing.value = false
            if (bmp != null) {
                initCorners(bmp.width.toFloat(), bmp.height.toFloat())
                warpDocument()
            }
        }
    }

    fun initCorners(w: Float, h: Float) {
        _corners.value = QuadCorners.defaultFor(w, h, marginFraction = 0.08f)
    }

    fun updateCorners(newCorners: QuadCorners) {
        _corners.value = newCorners
    }

    fun toggleMode() {
        _isCroppingMode.value = !_isCroppingMode.value
        if (!_isCroppingMode.value) {
            warpDocument()
        }
    }

    fun selectFilter(filter: ProcessingFilter) {
        _selectedFilter.value = filter
        warpDocument()
    }

    fun rotate90() {
        val current = _warpedBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val rotated = BitmapUtils.rotateBitmap(current, 90f)
            _warpedBitmap.value = rotated
        }
    }

    fun warpDocument() {
        val src = _sourceBitmap.value ?: return
        val currentCorners = _corners.value ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _isProcessing.value = true
            val warped = PerspectiveTransformEngine.warpQuadToRectangle(src, currentCorners)
            val filtered = ImageProcessingEngine.applyFilter(warped, _selectedFilter.value)
            _warpedBitmap.value = filtered
            _isProcessing.value = false
        }
    }

    fun exportDocument(asPdf: Boolean, onComplete: (StudioProject) -> Unit) {
        val doc = _warpedBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true

            val project = if (asPdf) {
                val filename = "doc_${System.currentTimeMillis()}.pdf"
                val pdfFile = PrintSheetEngine.exportToPdf(getApplication(), doc, filename)
                val thumbFile = BitmapUtils.saveToInternalStorage(
                    getApplication(),
                    Bitmap.createScaledBitmap(doc, 180, (180 * doc.height / doc.width), true),
                    "thumb_pdf_$filename.jpg"
                )
                StudioProject(
                    title = "Scanned Document PDF",
                    toolType = "document",
                    thumbnailPath = thumbFile.absolutePath,
                    filePath = pdfFile.absolutePath,
                    fileType = "pdf",
                    fileSizeBytes = pdfFile.length(),
                    dimensionsText = "A4 Page (PDF)"
                )
            } else {
                val filename = "doc_${System.currentTimeMillis()}.jpg"
                val file = BitmapUtils.saveToInternalStorage(getApplication(), doc, filename)
                val thumbFile = BitmapUtils.saveToInternalStorage(
                    getApplication(),
                    Bitmap.createScaledBitmap(doc, 180, (180 * doc.height / doc.width), true),
                    "thumb_$filename"
                )
                BitmapUtils.exportToGallery(getApplication(), doc, "PixelPro_ScannedDoc")
                StudioProject(
                    title = "Scanned Document Clean JPEG",
                    toolType = "document",
                    thumbnailPath = thumbFile.absolutePath,
                    filePath = file.absolutePath,
                    fileType = "jpeg",
                    fileSizeBytes = file.length(),
                    dimensionsText = "${doc.width} x ${doc.height} px"
                )
            }

            repository.insertProject(project)
            _isProcessing.value = false
            withContext(Dispatchers.Main) { onComplete(project) }
        }
    }

    private fun createSampleTiltedDocument(): Bitmap {
        val w = 900
        val h = 1200
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Wooden desk background
        canvas.drawColor(Color.parseColor("#334155"))

        // Slightly tilted white paper document
        canvas.save()
        canvas.rotate(2.5f, w / 2f, h / 2f)

        val paperPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
        canvas.drawRect(80f, 90f, (w - 80).toFloat(), (h - 90).toFloat(), paperPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 34f
            isFakeBoldText = true
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 24f
        }

        canvas.drawText("CERTIFICATE OF VERIFICATION", 150f, 200f, headerPaint)
        canvas.drawText("Document ID: DOC-2026-98421", 150f, 260f, textPaint)
        canvas.drawText("Issue Date: 12 October 2026", 150f, 310f, textPaint)
        canvas.drawText("Holder: Priya R. Patel", 150f, 360f, textPaint)
        canvas.drawText("Status: VERIFIED & AUTHENTICATED", 150f, 410f, textPaint)

        // Stamp / Seal circle
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D4ED8") // Blue ink stamp
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawCircle(w * 0.72f, h * 0.65f, 90f, stampPaint)
        val stampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D4ED8")
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("OFFICIAL SEAL", w * 0.72f, h * 0.65f, stampTextPaint)

        // Signature line
        canvas.drawLine(150f, (h * 0.75).toFloat(), 400f, (h * 0.75).toFloat(), textPaint)
        canvas.drawText("Authorized Signatory", 150f, (h * 0.75 + 40).toFloat(), textPaint)

        canvas.restore()
        return bmp
    }
}
