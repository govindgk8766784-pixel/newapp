package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PixelProDatabase
import com.example.data.model.PaperSize
import com.example.data.model.PassportPreset
import com.example.data.model.PrintSheetConfig
import com.example.data.model.StudioProject
import com.example.data.repository.StudioProjectRepository
import com.example.engine.BackgroundRemovalEngine
import com.example.engine.BitmapUtils
import com.example.engine.ImageProcessingEngine
import com.example.engine.PrintSheetEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PassportViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PixelProDatabase.getDatabase(application)
    private val repository = StudioProjectRepository(db.studioProjectDao())

    private val _sourceBitmap = MutableStateFlow<Bitmap?>(null)
    val sourceBitmap: StateFlow<Bitmap?> = _sourceBitmap.asStateFlow()

    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    val processedBitmap: StateFlow<Bitmap?> = _processedBitmap.asStateFlow()

    private val _sheetPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val sheetPreviewBitmap: StateFlow<Bitmap?> = _sheetPreviewBitmap.asStateFlow()

    private val _selectedPreset = MutableStateFlow(PassportPreset.ALL[0]) // Default: India Passport
    val selectedPreset: StateFlow<PassportPreset> = _selectedPreset.asStateFlow()

    private val _selectedBgColor = MutableStateFlow(Color.WHITE)
    val selectedBgColor: StateFlow<Int> = _selectedBgColor.asStateFlow()

    private val _showBiometricGuides = MutableStateFlow(true)
    val showBiometricGuides: StateFlow<Boolean> = _showBiometricGuides.asStateFlow()

    private val _brightness = MutableStateFlow(0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _contrast = MutableStateFlow(1f)
    val contrast: StateFlow<Float> = _contrast.asStateFlow()

    private val _skinSmoothing = MutableStateFlow(false)
    val skinSmoothing: StateFlow<Boolean> = _skinSmoothing.asStateFlow()

    private val _printConfig = MutableStateFlow(PrintSheetConfig(paperSize = PaperSize.FOUR_BY_SIX, photoCount = 6))
    val printConfig: StateFlow<PrintSheetConfig> = _printConfig.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _lastExportMessage = MutableStateFlow<String?>(null)
    val lastExportMessage: StateFlow<String?> = _lastExportMessage.asStateFlow()

    init {
        // Load default sample portrait for instant interactive experience
        loadSamplePortrait()
    }

    fun loadSamplePortrait() {
        viewModelScope.launch(Dispatchers.Default) {
            val sample = BitmapUtils.createSampleStudioPortrait()
            _sourceBitmap.value = sample
            processCurrentPassport()
        }
    }

    fun setSourceUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val bmp = BitmapUtils.decodeUriWithExif(getApplication(), uri)
            _sourceBitmap.value = bmp
            _isProcessing.value = false
            processCurrentPassport()
        }
    }

    fun selectPreset(preset: PassportPreset) {
        _selectedPreset.value = preset
        processCurrentPassport()
    }

    fun selectBgColor(color: Int) {
        _selectedBgColor.value = color
        processCurrentPassport()
    }

    fun toggleBiometricGuides() {
        _showBiometricGuides.value = !_showBiometricGuides.value
    }

    fun updateAdjustments(newBrightness: Float, newContrast: Float, smoothSkin: Boolean) {
        _brightness.value = newBrightness
        _contrast.value = newContrast
        _skinSmoothing.value = smoothSkin
        processCurrentPassport()
    }

    fun updatePrintConfig(config: PrintSheetConfig) {
        _printConfig.value = config
        generatePrintSheetPreview()
    }

    fun processCurrentPassport() {
        val src = _sourceBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _isProcessing.value = true

            // 1. Crop to target preset ratio
            val cropped = BitmapUtils.cropToAspectRatio(src, _selectedPreset.value.aspectRatio)

            // 2. Color adjustments
            var adjusted = ImageProcessingEngine.applyAdjustments(
                cropped,
                brightness = _brightness.value,
                contrast = _contrast.value,
                sharpness = 0.25f
            )

            if (_skinSmoothing.value) {
                adjusted = ImageProcessingEngine.applyFilter(adjusted, com.example.data.model.ProcessingFilter.STUDIO_ENHANCE)
            }

            // 3. Background replacement if color is not pure white default or user changed
            val finalPhoto = if (_selectedBgColor.value != Color.WHITE) {
                val mask = BackgroundRemovalEngine.generateCutoutMask(adjusted)
                BackgroundRemovalEngine.compositeImage(adjusted, mask, bgType = "solid", bgColor = _selectedBgColor.value)
            } else {
                adjusted
            }

            _processedBitmap.value = finalPhoto
            _isProcessing.value = false
            generatePrintSheetPreview()
        }
    }

    private fun generatePrintSheetPreview() {
        val photo = _processedBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val sheet = PrintSheetEngine.generatePrintSheetBitmap(photo, _printConfig.value)
            _sheetPreviewBitmap.value = sheet
        }
    }

    fun exportSinglePhoto(onComplete: (StudioProject) -> Unit) {
        val photo = _processedBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val filename = "passport_${System.currentTimeMillis()}.jpg"
            val file = BitmapUtils.saveToInternalStorage(getApplication(), photo, filename)

            val thumbFile = BitmapUtils.saveToInternalStorage(
                getApplication(),
                Bitmap.createScaledBitmap(photo, 150, (150 / _selectedPreset.value.aspectRatio).toInt(), true),
                "thumb_$filename"
            )

            // Export to public gallery
            BitmapUtils.exportToGallery(getApplication(), photo, "PixelPro_${_selectedPreset.value.name}")

            val project = StudioProject(
                title = "${_selectedPreset.value.name} Photo",
                toolType = "passport",
                thumbnailPath = thumbFile.absolutePath,
                filePath = file.absolutePath,
                fileType = "jpeg",
                fileSizeBytes = file.length(),
                dimensionsText = _selectedPreset.value.dimensionDisplay
            )
            repository.insertProject(project)

            _isProcessing.value = false
            _lastExportMessage.value = "Passport photo saved and added to Gallery!"
            withContext(Dispatchers.Main) { onComplete(project) }
        }
    }

    fun exportPrintSheet(asPdf: Boolean, onComplete: (StudioProject) -> Unit) {
        val photo = _processedBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val sheet = PrintSheetEngine.generatePrintSheetBitmap(photo, _printConfig.value)

            val project = if (asPdf) {
                val filename = "passport_sheet_${System.currentTimeMillis()}.pdf"
                val pdfFile = PrintSheetEngine.exportToPdf(getApplication(), sheet, filename)
                val thumbFile = BitmapUtils.saveToInternalStorage(
                    getApplication(),
                    Bitmap.createScaledBitmap(sheet, 200, (200 * sheet.height / sheet.width), true),
                    "thumb_pdf_$filename.jpg"
                )
                StudioProject(
                    title = "Passport Sheet (${_printConfig.value.photoCount} Photos)",
                    toolType = "passport",
                    thumbnailPath = thumbFile.absolutePath,
                    filePath = pdfFile.absolutePath,
                    fileType = "pdf",
                    fileSizeBytes = pdfFile.length(),
                    dimensionsText = "${_printConfig.value.paperSize.title} PDF"
                )
            } else {
                val filename = "passport_sheet_${System.currentTimeMillis()}.jpg"
                val file = BitmapUtils.saveToInternalStorage(getApplication(), sheet, filename)
                val thumbFile = BitmapUtils.saveToInternalStorage(
                    getApplication(),
                    Bitmap.createScaledBitmap(sheet, 200, (200 * sheet.height / sheet.width), true),
                    "thumb_$filename"
                )
                BitmapUtils.exportToGallery(getApplication(), sheet, "PixelPro_PrintSheet_${_printConfig.value.photoCount}Photos")
                StudioProject(
                    title = "Passport Sheet (${_printConfig.value.photoCount} Photos)",
                    toolType = "passport",
                    thumbnailPath = thumbFile.absolutePath,
                    filePath = file.absolutePath,
                    fileType = "jpeg",
                    fileSizeBytes = file.length(),
                    dimensionsText = "${_printConfig.value.paperSize.title} (300 DPI)"
                )
            }

            repository.insertProject(project)
            _isProcessing.value = false
            _lastExportMessage.value = if (asPdf) "Print Sheet PDF generated!" else "Print Sheet saved to Gallery!"
            withContext(Dispatchers.Main) { onComplete(project) }
        }
    }
}
