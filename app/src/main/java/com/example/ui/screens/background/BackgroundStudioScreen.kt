package com.example.ui.screens.background

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ColorPickerRow
import com.example.ui.components.StudioTopAppBar
import com.example.viewmodel.BackgroundViewModel

@Composable
fun BackgroundStudioScreen(
    viewModel: BackgroundViewModel,
    onBackClick: () -> Unit,
    onExportComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val srcBmp by viewModel.sourceBitmap.collectAsStateWithLifecycle()
    val resultBmp by viewModel.resultBitmap.collectAsStateWithLifecycle()
    val bgType by viewModel.bgType.collectAsStateWithLifecycle()
    val bgColor by viewModel.bgColor.collectAsStateWithLifecycle()
    val tolerance by viewModel.tolerance.collectAsStateWithLifecycle()
    val brushMode by viewModel.brushMode.collectAsStateWithLifecycle()
    val brushSize by viewModel.brushSize.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

    var showOriginal by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { viewModel.setSourceUri(it) } }

    Column(modifier = Modifier.fillMaxSize()) {
        StudioTopAppBar(title = "Background Changer", onBackClick = onBackClick)

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Cutout Canvas with touch brush support
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cutout_preview_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (bgType == "transparent") Color(0xFF1E293B) else Color(bgColor))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                            .pointerInput(brushMode, brushSize) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    viewModel.applyBrushStroke(
                                        touchX = change.position.x,
                                        touchY = change.position.y,
                                        canvasW = size.width.toFloat(),
                                        canvasH = size.height.toFloat()
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val displayBmp = if (showOriginal) srcBmp else resultBmp
                        displayBmp?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Cutout Result",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
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
                            onClick = { showOriginal = !showOriginal },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("toggle_before_after_button")
                        ) {
                            Text(if (showOriginal) "Viewing: Original" else "Viewing: Cutout", style = MaterialTheme.typography.labelSmall)
                        }

                        Text(
                            text = if (bgType == "transparent") "Transparent PNG" else "Solid Backdrop",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    modifier = Modifier.weight(1f).testTag("bg_pick_photo_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Photo")
                }

                OutlinedButton(
                    onClick = { viewModel.loadSamplePortrait() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("bg_sample_photo_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sample")
                }
            }

            // Color Palette Row
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    ColorPickerRow(
                        selectedColor = if (bgType == "transparent") Color.Transparent else Color(bgColor),
                        onColorSelected = { color, isTransparent ->
                            viewModel.setBgColor(color.toArgb(), isTransparent)
                        }
                    )
                }
            }

            // Brush Touch-up & Tolerance Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Manual Edge Touch-Up Brush", style = MaterialTheme.typography.labelLarge)
                    Text("Drag on the preview above to erase background or restore edges", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = brushMode == "erase",
                            onClick = { viewModel.setBrushMode("erase") },
                            label = { Text("Erase Background", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("brush_mode_erase")
                        )
                        FilterChip(
                            selected = brushMode == "restore",
                            onClick = { viewModel.setBrushMode("restore") },
                            label = { Text("Restore Edge", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("brush_mode_restore")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Brush Size", style = MaterialTheme.typography.bodySmall)
                        Text("${brushSize.toInt()} px", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = brushSize,
                        onValueChange = { viewModel.setBrushSize(it) },
                        valueRange = 15f..80f,
                        modifier = Modifier.testTag("brush_size_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Edge Tolerance", style = MaterialTheme.typography.bodySmall)
                        Text(String.format("%.2f", tolerance), style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = tolerance,
                        onValueChange = { viewModel.setTolerance(it) },
                        valueRange = 0.10f..0.45f,
                        modifier = Modifier.testTag("tolerance_slider")
                    )
                }
            }

            // Export Action
            Button(
                onClick = {
                    viewModel.exportResult {
                        onExportComplete("Cutout exported and saved to Gallery!")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("export_cutout_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (bgType == "transparent") "Export Transparent PNG" else "Export Backdrop JPEG")
            }
        }
    }
}
