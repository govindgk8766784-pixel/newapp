package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PixelProDatabase
import com.example.data.model.StudioProject
import com.example.data.repository.StudioProjectRepository
import com.example.engine.BitmapUtils
import com.example.ui.navigation.StudioDestination
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PixelProDatabase.getDatabase(application)
    val repository = StudioProjectRepository(db.studioProjectDao())

    private val _currentDestination = MutableStateFlow(StudioDestination.DASHBOARD)
    val currentDestination: StateFlow<StudioDestination> = _currentDestination.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    val allProjects: StateFlow<List<StudioProject>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentProjects: StateFlow<List<StudioProject>> = repository.getRecentProjects(5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalProjectsCount: StateFlow<Int> = repository.totalCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun navigateTo(destination: StudioDestination) {
        _currentDestination.value = destination
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _userMessage.emit(message)
        }
    }

    fun deleteProject(project: StudioProject) {
        viewModelScope.launch {
            // Delete file from disk if exists
            project.filePath?.let { path ->
                try { File(path).delete() } catch (e: Exception) { e.printStackTrace() }
            }
            project.thumbnailPath?.let { path ->
                try { File(path).delete() } catch (e: Exception) { e.printStackTrace() }
            }
            repository.deleteProject(project)
            _userMessage.emit("Project deleted")
        }
    }

    fun exportToGallery(project: StudioProject) {
        viewModelScope.launch {
            project.filePath?.let { path ->
                val file = File(path)
                if (file.exists() && project.fileType != "pdf") {
                    val bmp = android.graphics.BitmapFactory.decodeFile(path)
                    if (bmp != null) {
                        val uri = BitmapUtils.exportToGallery(getApplication(), bmp, project.title)
                        if (uri != null) {
                            _userMessage.emit("Saved to device gallery!")
                        } else {
                            _userMessage.emit("Failed to save to gallery")
                        }
                    }
                } else if (file.exists() && project.fileType == "pdf") {
                    BitmapUtils.shareFile(getApplication(), file, "application/pdf", "Share ${project.title}")
                }
            }
        }
    }

    fun shareProject(project: StudioProject) {
        project.filePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val mime = if (project.fileType == "pdf") "application/pdf" else "image/jpeg"
                BitmapUtils.shareFile(getApplication(), file, mime, "Share ${project.title}")
            }
        }
    }
}
