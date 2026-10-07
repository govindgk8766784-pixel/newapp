package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PixelProDatabase
import com.example.data.model.StudioProject
import com.example.data.repository.StudioProjectRepository
import com.example.engine.BackgroundRemovalEngine
import com.example.engine.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackgroundViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PixelProDatabase.getDatabase(application)
    private val repository = StudioProjectRepository(db.studioProjectDao())

    private val _sourceBitmap = MutableStateFlow<Bitmap?>(null)
    val sourceBitmap: StateFlow<Bitmap?> = _sourceBitmap.asStateFlow()

    private val _maskBitmap = MutableStateFlow<Bitmap?>(null)
    val maskBitmap: StateFlow<Bitmap?> = _maskBitmap.asStateFlow()

    private val _resultBitmap = MutableStateFlow<Bitmap?>(null)
    val resultBitmap: StateFlow<Bitmap?> = _resultBitmap.asStateFlow()

    private val _bgType = MutableStateFlow("solid") // "solid", "transparent", "gradient"
    val bgType: StateFlow<String> = _bgType.asStateFlow()

    private val _bgColor = MutableStateFlow(Color.WHITE)
    val bgColor: StateFlow<Int> = _bgColor.asStateFlow()

    private val _tolerance = MutableStateFlow(0.22f)
    val tolerance: StateFlow<Float> = _tolerance.asStateFlow()

    private val _brushMode = MutableStateFlow("erase") // "erase" or "restore"
    val brushMode: StateFlow<String> = _brushMode.asStateFlow()

    private val _brushSize = MutableStateFlow(35f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        loadSamplePortrait()
    }

    fun loadSamplePortrait() {
        viewModelScope.launch(Dispatchers.Default) {
            val sample = BitmapUtils.createSampleStudioPortrait()
            _sourceBitmap.value = sample
            generateCutout()
        }
    }

    fun setSourceUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val bmp = BitmapUtils.decodeUriWithExif(getApplication(), uri)
            _sourceBitmap.value = bmp
            _isProcessing.value = false
            generateCutout()
        }
    }

    fun setTolerance(newTolerance: Float) {
        _tolerance.value = newTolerance
        generateCutout()
    }

    fun setBgType(type: String) {
        _bgType.value = type
        recomposite()
    }

    fun setBgColor(color: Int, isTransparent: Boolean) {
        if (isTransparent) {
            _bgType.value = "transparent"
        } else {
            _bgType.value = "solid"
            _bgColor.value = color
        }
        recomposite()
    }

    fun setBrushMode(mode: String) {
        _brushMode.value = mode
    }

    fun setBrushSize(size: Float) {
        _brushSize.value = size
    }

    fun applyBrushStroke(touchX: Float, touchY: Float, canvasW: Float, canvasH: Float) {
        val mask = _maskBitmap.value ?: return
        // Map touch coordinates to mask bitmap coordinates
        val scaleX = mask.width / canvasW
        val scaleY = mask.height / canvasH
        val maskX = touchX * scaleX
        val maskY = touchY * scaleY
        val radius = _brushSize.value * scaleX

        BackgroundRemovalEngine.applyTouchUpStroke(mask, maskX, maskY, radius, _brushMode.value)
        recomposite()
    }

    fun generateCutout() {
        val src = _sourceBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _isProcessing.value = true
            val mask = BackgroundRemovalEngine.generateCutoutMask(src, _tolerance.value)
            _maskBitmap.value = mask
            recompositeInternal(src, mask)
            _isProcessing.value = false
        }
    }

    fun recomposite() {
        val src = _sourceBitmap.value ?: return
        val mask = _maskBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            recompositeInternal(src, mask)
        }
    }

    private fun recompositeInternal(src: Bitmap, mask: Bitmap) {
        val result = BackgroundRemovalEngine.compositeImage(
            src = src,
            mask = mask,
            bgType = _bgType.value,
            bgColor = _bgColor.value
        )
        _resultBitmap.value = result
    }

    fun exportResult(onComplete: (StudioProject) -> Unit) {
        val result = _resultBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val isPng = _bgType.value == "transparent"
            val ext = if (isPng) "png" else "jpg"
            val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

            val filename = "cutout_${System.currentTimeMillis()}.$ext"
            val file = BitmapUtils.saveToInternalStorage(getApplication(), result, filename, format = format)

            val thumbFile = BitmapUtils.saveToInternalStorage(
                getApplication(),
                Bitmap.createScaledBitmap(result, 180, (180 * result.height / result.width), true),
                "thumb_$filename"
            )

            BitmapUtils.exportToGallery(getApplication(), result, "PixelPro_Cutout")

            val project = StudioProject(
                title = if (isPng) "Transparent Cutout PNG" else "Studio Background Cutout",
                toolType = "background",
                thumbnailPath = thumbFile.absolutePath,
                filePath = file.absolutePath,
                fileType = ext,
                fileSizeBytes = file.length(),
                dimensionsText = "${result.width} x ${result.height} px"
            )
            repository.insertProject(project)

            _isProcessing.value = false
            withContext(Dispatchers.Main) { onComplete(project) }
        }
    }
}
