package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "studio_projects")
data class StudioProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val toolType: String, // "passport", "id_card", "background", "document", "print_sheet"
    val dateCreated: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val filePath: String? = null,
    val fileType: String = "jpeg", // "jpeg", "png", "pdf"
    val fileSizeBytes: Long = 0,
    val dimensionsText: String = "",
    val extraNotes: String? = null
)
