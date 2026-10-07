package com.example.ui.screens.document

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProcessingFilter
import com.example.ui.components.QuadCropOverlay
import com.example.ui.components.StudioTopAppBar
import com.example.viewmodel.DocumentViewModel

@Composable
fun DocumentStudioScreen(
    viewModel: DocumentViewModel,
    onBackClick: () -> Unit,
    onExportComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val srcBmp by viewModel.sourceBitmap.collectAsStateWithLifecycle()
    val corners by viewModel.corners.collectAsStateWithLifecycle()
    val warpedBmp by viewModel.warpedBitmap.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val isCroppingMode by viewModel.isCroppingMode.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { viewModel.setSourceUri(it) } }

    Column(modifier = Modifier.fillMaxSize()) {
        StudioTopAppBar(title = "Document Scanner & Cropper", onBackClick = onBackClick)

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Document Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("document_preview_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCroppingMode) {
                            // Cropping mode: display source image and interactive quad handles
                            srcBmp?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Source Document",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            corners?.let { quad ->
                                QuadCropOverlay(
                                    corners = quad,
                                    onCornersChanged = { viewModel.updateCorners(it) }
                                )
                            }
                        } else {
                            // Straightened mode: display warped and filtered result
                            warpedBmp?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Straightened Document",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.toggleMode() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("toggle_doc_mode_button")
                        ) {
                            Icon(
                                imageVector = if (isCroppingMode) Icons.Default.Visibility else Icons.Default.Crop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isCroppingMode) "Preview Straightened" else "Edit 4 Corners",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        IconButton(
                            onClick = { viewModel.rotate90() },
                            modifier = Modifier.testTag("doc_rotate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Rotate 90 degrees",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Photo Pick Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f).testTag("doc_pick_photo_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Document")
                }

                OutlinedButton(
                    onClick = { viewModel.loadSampleDocument() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("doc_sample_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sample")
                }
            }

            // Enhancement Filters Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Document Scan Filter", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ProcessingFilter.CLEAN_PAPER,
                            ProcessingFilter.MAGIC_COLOR,
                            ProcessingFilter.BW_PHOTOCOPY,
                            ProcessingFilter.GRAYSCALE,
                            ProcessingFilter.ORIGINAL
                        ).forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { viewModel.selectFilter(filter) },
                                label = { Text(filter.title, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("doc_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Export Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.exportDocument(asPdf = false) {
                            onExportComplete("Scanned document saved as JPEG!")
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("export_doc_jpeg_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save JPEG")
                }

                Button(
                    onClick = {
                        viewModel.exportDocument(asPdf = true) {
                            onExportComplete("Scanned document PDF generated!")
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("export_doc_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export PDF")
                }
            }
        }
    }
}
