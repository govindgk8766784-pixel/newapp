package com.example.ui.screens.idcard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.IdCardPreset
import com.example.data.model.ProcessingFilter
import com.example.ui.components.StudioTopAppBar
import com.example.viewmodel.IdCardViewModel

@Composable
fun IdCardStudioScreen(
    viewModel: IdCardViewModel,
    onBackClick: () -> Unit,
    onExportComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val frontBmp by viewModel.frontBitmap.collectAsStateWithLifecycle()
    val backBmp by viewModel.backBitmap.collectAsStateWithLifecycle()
    val combinedBmp by viewModel.combinedPreview.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val layoutMode by viewModel.layoutMode.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

    val frontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { viewModel.setFrontUri(it) } }

    val backPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { viewModel.setBackUri(it) } }

    Column(modifier = Modifier.fillMaxSize()) {
        StudioTopAppBar(title = "Aadhaar / ID Card Studio", onBackClick = onBackClick)

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Preset selector chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IdCardPreset.ALL.forEach { preset ->
                    val isSelected = selectedPreset.id == preset.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectPreset(preset) },
                        label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.testTag("id_preset_${preset.id}")
                    )
                }
            }

            // Dual Side Upload Slots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Front Side Slot
                CardSlot(
                    title = "Front Side",
                    bitmap = frontBmp,
                    aspectRatio = selectedPreset.aspectRatio,
                    testTag = "upload_front_card_button",
                    onClick = {
                        frontPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                )

                // Back Side Slot
                if (selectedPreset.isDualSided) {
                    CardSlot(
                        title = "Back Side",
                        bitmap = backBmp,
                        aspectRatio = selectedPreset.aspectRatio,
                        testTag = "upload_back_card_button",
                        onClick = {
                            backPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Document Enhancement Filters Row
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Enhancement Filter", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ProcessingFilter.MAGIC_COLOR,
                            ProcessingFilter.CLEAN_PAPER,
                            ProcessingFilter.BW_PHOTOCOPY,
                            ProcessingFilter.GRAYSCALE,
                            ProcessingFilter.ORIGINAL
                        ).forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { viewModel.selectFilter(filter) },
                                label = { Text(filter.title, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("id_filter_${filter.name.lowercase()}")
                            )
                        }
                    }

                    if (selectedPreset.isDualSided) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Arrangement Layout", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = layoutMode == "stacked",
                                onClick = { viewModel.setLayoutMode("stacked") },
                                label = { Text("Stacked (Vertical)", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("layout_stacked_chip")
                            )
                            FilterChip(
                                selected = layoutMode == "side_by_side",
                                onClick = { viewModel.setLayoutMode("side_by_side") },
                                label = { Text("Side-by-Side", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(Icons.Default.ViewColumn, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("layout_side_by_side_chip")
                            )
                        }
                    }
                }
            }

            // Combined Sheet Preview Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("combined_id_preview_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Print-Ready Combined Sheet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        combinedBmp?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Combined ID Document",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${selectedPreset.name} • ${selectedPreset.dimensionDisplay}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                            onExportComplete("ID Card document saved as JPEG!")
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("export_id_card_jpeg_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save JPEG")
                }

                Button(
                    onClick = {
                        viewModel.exportDocument(asPdf = true) {
                            onExportComplete("ID Card document generated as PDF!")
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("export_id_card_pdf_button"),
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

@Composable
private fun CardSlot(
    title: String,
    bitmap: android.graphics.Bitmap?,
    aspectRatio: Float,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Upload",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tap to change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
