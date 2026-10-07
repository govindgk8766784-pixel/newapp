package com.example.ui.screens.passport

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PaperSize
import com.example.data.model.PassportPreset
import com.example.data.model.PrintSheetConfig
import com.example.ui.components.BiometricGuideOverlay
import com.example.ui.components.ColorPickerRow
import com.example.ui.components.StudioTopAppBar
import com.example.viewmodel.PassportViewModel

@Composable
fun PassportStudioScreen(
    viewModel: PassportViewModel,
    onBackClick: () -> Unit,
    onExportComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val processedBmp by viewModel.processedBitmap.collectAsStateWithLifecycle()
    val sheetBmp by viewModel.sheetPreviewBitmap.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()
    val selectedBgColor by viewModel.selectedBgColor.collectAsStateWithLifecycle()
    val showGuides by viewModel.showBiometricGuides.collectAsStateWithLifecycle()
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()
    val contrast by viewModel.contrast.collectAsStateWithLifecycle()
    val skinSmooth by viewModel.skinSmoothing.collectAsStateWithLifecycle()
    val printConfig by viewModel.printConfig.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }

    // Android Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSourceUri(it) }
    }

    ScaffoldWithTopBar(
        title = "Passport Photo Studio",
        onBackClick = onBackClick
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Preset Selection Row
            PresetSelectionRow(
                selectedPreset = selectedPreset,
                onPresetSelected = { viewModel.selectPreset(it) }
            )

            // Main Preview Canvas with Biometric Guide
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("passport_preview_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .aspectRatio(selectedPreset.aspectRatio)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(selectedBgColor))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        processedBmp?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Passport Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        BiometricGuideOverlay(showGuides = showGuides)

                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedPreset.dimensionDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Biometric Guide",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = showGuides,
                                onCheckedChange = { viewModel.toggleBiometricGuides() },
                                modifier = Modifier.testTag("toggle_biometric_guides")
                            )
                        }
                    }
                }
            }

            // Quick Photo Source Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_photo_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Photo", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { viewModel.loadSamplePortrait() },
                    modifier = Modifier.testTag("load_sample_portrait_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sample", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Tabs: Retouch & Backdrop vs Print Sheet
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Color & Retouch") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Print Sheet") },
                    icon = { Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (activeTab == 0) {
                // Color & Retouch Panel
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ColorPickerRow(
                            selectedColor = Color(selectedBgColor),
                            onColorSelected = { color, _ ->
                                viewModel.selectBgColor(color.toArgb())
                            }
                        )

                        // Retouch Sliders
                        Text("Portrait Adjustments", style = MaterialTheme.typography.labelLarge)

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Brightness", style = MaterialTheme.typography.bodySmall)
                                Text("${brightness.toInt()}", style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = brightness,
                                onValueChange = { viewModel.updateAdjustments(it, contrast, skinSmooth) },
                                valueRange = -50f..50f,
                                modifier = Modifier.testTag("slider_brightness")
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Contrast", style = MaterialTheme.typography.bodySmall)
                                Text(String.format("%.1fx", contrast), style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = contrast,
                                onValueChange = { viewModel.updateAdjustments(brightness, it, skinSmooth) },
                                valueRange = 0.7f..1.5f,
                                modifier = Modifier.testTag("slider_contrast")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Studio Skin Beautify", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Soft smoothing filter preserving eye sharpness", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = skinSmooth,
                                onCheckedChange = { viewModel.updateAdjustments(brightness, contrast, it) },
                                modifier = Modifier.testTag("switch_skin_beautify")
                            )
                        }
                    }
                }
            } else {
                // Print Sheet Panel
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Paper Size", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaperSize.values().forEach { paper ->
                                FilterChip(
                                    selected = printConfig.paperSize == paper,
                                    onClick = { viewModel.updatePrintConfig(printConfig.copy(paperSize = paper)) },
                                    label = { Text(paper.title, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Text("Photo Count on Sheet", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(4, 6, 8, 12, 16, 32).forEach { count ->
                                FilterChip(
                                    selected = printConfig.photoCount == count,
                                    onClick = { viewModel.updatePrintConfig(printConfig.copy(photoCount = count)) },
                                    label = { Text("$count Photos", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        // Sheet Miniature Preview
                        sheetBmp?.let { sheet ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2E8F0))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = sheet.asImageBitmap(),
                                    contentDescription = "Print Sheet Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
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
                        viewModel.exportSinglePhoto { project ->
                            onExportComplete("Single passport photo saved!")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_single_photo_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Photo")
                }

                Button(
                    onClick = {
                        viewModel.exportPrintSheet(asPdf = true) { project ->
                            onExportComplete("Print Sheet PDF generated!")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_print_sheet_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print PDF")
                }
            }
        }
    }
}

@Composable
private fun PresetSelectionRow(
    selectedPreset: PassportPreset,
    onPresetSelected: (PassportPreset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PassportPreset.ALL.forEach { preset ->
            val isSelected = selectedPreset.id == preset.id
            FilterChip(
                selected = isSelected,
                onClick = { onPresetSelected(preset) },
                label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null,
                modifier = Modifier.testTag("preset_chip_${preset.id}")
            )
        }
    }
}

@Composable
fun ScaffoldWithTopBar(
    title: String,
    onBackClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        StudioTopAppBar(title = title, onBackClick = onBackClick)
        content()
    }
}
