package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PixelProDatabase
import com.example.data.model.IdCardPreset
import com.example.data.model.ProcessingFilter
import com.example.data.model.StudioProject
import com.example.data.repository.StudioProjectRepository
import com.example.engine.BitmapUtils
import com.example.engine.ImageProcessingEngine
import com.example.engine.PrintSheetEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdCardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PixelProDatabase.getDatabase(application)
    private val repository = StudioProjectRepository(db.studioProjectDao())

    private val _frontBitmap = MutableStateFlow<Bitmap?>(null)
    val frontBitmap: StateFlow<Bitmap?> = _frontBitmap.asStateFlow()

    private val _backBitmap = MutableStateFlow<Bitmap?>(null)
    val backBitmap: StateFlow<Bitmap?> = _backBitmap.asStateFlow()

    private val _combinedPreview = MutableStateFlow<Bitmap?>(null)
    val combinedPreview: StateFlow<Bitmap?> = _combinedPreview.asStateFlow()

    private val _selectedPreset = MutableStateFlow(IdCardPreset.ALL[0]) // Aadhaar Card
    val selectedPreset: StateFlow<IdCardPreset> = _selectedPreset.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ProcessingFilter.MAGIC_COLOR)
    val selectedFilter: StateFlow<ProcessingFilter> = _selectedFilter.asStateFlow()

    private val _layoutMode = MutableStateFlow("stacked") // "stacked" (vertical) or "side_by_side" (horizontal)
    val layoutMode: StateFlow<String> = _layoutMode.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        loadSampleIdCards()
    }

    fun loadSampleIdCards() {
        viewModelScope.launch(Dispatchers.Default) {
            _frontBitmap.value = createSampleIdCard(isFront = true)
            _backBitmap.value = createSampleIdCard(isFront = false)
            updateCombinedDocument()
        }
    }

    fun setFrontUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val bmp = BitmapUtils.decodeUriWithExif(getApplication(), uri)
            _frontBitmap.value = bmp
            _isProcessing.value = false
            updateCombinedDocument()
        }
    }

    fun setBackUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val bmp = BitmapUtils.decodeUriWithExif(getApplication(), uri)
            _backBitmap.value = bmp
            _isProcessing.value = false
            updateCombinedDocument()
        }
    }

    fun selectPreset(preset: IdCardPreset) {
        _selectedPreset.value = preset
        updateCombinedDocument()
    }

    fun selectFilter(filter: ProcessingFilter) {
        _selectedFilter.value = filter
        updateCombinedDocument()
    }

    fun setLayoutMode(mode: String) {
        _layoutMode.value = mode
        updateCombinedDocument()
    }

    fun updateCombinedDocument() {
        val front = _frontBitmap.value ?: return
        val back = _backBitmap.value

        viewModelScope.launch(Dispatchers.Default) {
            _isProcessing.value = true

            // Enhance front and back using selected filter
            val frontEnhanced = ImageProcessingEngine.applyFilter(front, _selectedFilter.value)
            val backEnhanced = back?.let { ImageProcessingEngine.applyFilter(it, _selectedFilter.value) }

            // Combine into unified sheet
            val combined = combineCardBitmaps(frontEnhanced, backEnhanced, _selectedPreset.value, _layoutMode.value)
            _combinedPreview.value = combined
            _isProcessing.value = false
        }
    }

    private fun combineCardBitmaps(
        front: Bitmap,
        back: Bitmap?,
        preset: IdCardPreset,
        mode: String
    ): Bitmap {
        val targetRatio = preset.aspectRatio
        val frontCropped = BitmapUtils.cropToAspectRatio(front, targetRatio)
        val backCropped = back?.let { BitmapUtils.cropToAspectRatio(it, targetRatio) }

        val cardW = 1000
        val cardH = (cardW / targetRatio).toInt()

        val margin = 50
        val headerHeight = 70

        val (sheetW, sheetH) = if (backCropped == null || !preset.isDualSided) {
            // Single card sheet
            Pair(cardW + 2 * margin, cardH + 2 * margin + headerHeight)
        } else if (mode == "side_by_side") {
            Pair(cardW * 2 + 3 * margin, cardH + 2 * margin + headerHeight)
        } else {
            // Stacked (Vertical)
            Pair(cardW + 2 * margin, cardH * 2 + 3 * margin + headerHeight * 2)
        }

        val sheet = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 32f
            isFakeBoldText = true
        }

        // Draw Front
        val frontLeft = margin.toFloat()
        val frontTop = (margin + headerHeight).toFloat()
        val frontRect = RectF(frontLeft, frontTop, frontLeft + cardW, frontTop + cardH)

        canvas.drawText("${preset.name} - FRONT SIDE", frontLeft + 10f, frontTop - 20f, titlePaint)
        canvas.drawBitmap(frontCropped, null, frontRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.drawRoundRect(frontRect, 16f, 16f, borderPaint)

        // Draw Back if present
        if (backCropped != null && preset.isDualSided) {
            val (backLeft, backTop) = if (mode == "side_by_side") {
                Pair((cardW + 2 * margin).toFloat(), (margin + headerHeight).toFloat())
            } else {
                Pair(margin.toFloat(), (frontTop + cardH + margin + headerHeight))
            }
            val backRect = RectF(backLeft, backTop, backLeft + cardW, backTop + cardH)

            canvas.drawText("${preset.name} - BACK SIDE", backLeft + 10f, backTop - 20f, titlePaint)
            canvas.drawBitmap(backCropped, null, backRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.drawRoundRect(backRect, 16f, 16f, borderPaint)
        }

        return sheet
    }

    fun exportDocument(asPdf: Boolean, onComplete: (StudioProject) -> Unit) {
        val document = _combinedPreview.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true

            val project = if (asPdf) {
                val filename = "id_card_${System.currentTimeMillis()}.pdf"
                val pdfFile = PrintSheetEngine.exportToPdf(getApplication(), document, filename)
                val thumbFile = BitmapUtils.saveToInternalStorage(
                    getApplication(),
                    Bitmap.createScaledBitmap(document, 200, (200 * document.height / document.width), true),
                    "thumb_pdf_$filename.jpg"
                )
                StudioProject(
                    title = "${_selectedPreset.value.name} Card Document",
                    toolType = "id_card",
                    thumbnailPath = thumbFile.absolutePath,
                    filePath = pdfFile.absolutePath,
                    fileType = "pdf",
                    fileSizeBytes = pdfFile.length(),
                    dimensionsText = "${_selectedPreset.value.dimensionDisplay} (PDF)"
                )
            } else {
                val filename = "id_card_${System.currentTimeMillis()}.jpg"
                val file = BitmapUtils.saveToInternalStorage(getApplication(), document, filename)
                val thumbFile = BitmapUtils.saveToInternalStorage(
                    getApplication(),
                    Bitmap.createScaledBitmap(document, 200, (200 * document.height / document.width), true),
                    "thumb_$filename"
                )
                BitmapUtils.exportToGallery(getApplication(), document, "PixelPro_${_selectedPreset.value.name}")
                StudioProject(
                    title = "${_selectedPreset.value.name} Card Document",
                    toolType = "id_card",
                    thumbnailPath = thumbFile.absolutePath,
                    filePath = file.absolutePath,
                    fileType = "jpeg",
                    fileSizeBytes = file.length(),
                    dimensionsText = "${_selectedPreset.value.dimensionDisplay} (JPEG)"
                )
            }

            repository.insertProject(project)
            _isProcessing.value = false
            withContext(Dispatchers.Main) { onComplete(project) }
        }
    }

    private fun createSampleIdCard(isFront: Boolean): Bitmap {
        val w = 856
        val h = 540
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        // Top Gov Banner
        val bannerPaint = Paint().apply { color = Color.parseColor("#E06D14") } // saffron band
        canvas.drawRect(0f, 0f, w.toFloat(), 40f, bannerPaint)

        val bottomPaint = Paint().apply { color = Color.parseColor("#15803D") } // green band
        canvas.drawRect(0f, (h - 30).toFloat(), w.toFloat(), h.toFloat(), bottomPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 28f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 22f
        }

        if (isFront) {
            canvas.drawText("GOVERNMENT OF INDIA", 80f, 90f, textPaint)
            canvas.drawText("Unique Identification Authority of India", 80f, 125f, subPaint)

            // Photo box
            val photoBox = RectF(60f, 160f, 260f, 410f)
            val boxPaint = Paint().apply { color = Color.parseColor("#CBD5E1") }
            canvas.drawRect(photoBox, boxPaint)
            val sampleFace = BitmapUtils.createSampleStudioPortrait(200, 250)
            canvas.drawBitmap(sampleFace, null, photoBox, null)

            // Text entries
            canvas.drawText("Name: Rahul S. Sharma", 300f, 200f, textPaint)
            canvas.drawText("DOB: 15/08/1992", 300f, 245f, subPaint)
            canvas.drawText("Gender: MALE", 300f, 290f, subPaint)

            val aadharNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B91C1C")
                textSize = 36f
                isFakeBoldText = true
            }
            canvas.drawText("XXXX  XXXX  4928", 300f, 380f, aadharNumPaint)
        } else {
            canvas.drawText("Address / Details", 80f, 90f, textPaint)
            canvas.drawText("S/O: Suresh Sharma", 80f, 150f, subPaint)
            canvas.drawText("House No. 42B, Sector 18, City Center", 80f, 195f, subPaint)
            canvas.drawText("New Delhi, Delhi - 110001", 80f, 240f, subPaint)

            // Barcode placeholder
            val barPaint = Paint().apply { color = Color.parseColor("#1E293B") }
            for (i in 0..30) {
                val bx = 80f + i * 20f
                canvas.drawRect(bx, 300f, bx + if (i % 2 == 0) 8f else 14f, 420f, barPaint)
            }
        }

        return bmp
    }
}
