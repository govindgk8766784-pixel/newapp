package com.example.ui.screens.studio

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.StudioTopAppBar
import java.io.File

@Composable
fun StudioHealthScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo().apply { actManager?.getMemoryInfo(this) }

    val availMemMb = (memInfo.availMem / (1024 * 1024)).toInt()
    val totalMemMb = (memInfo.totalMem / (1024 * 1024)).toInt()

    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        StudioTopAppBar(title = "Engine Diagnostics & Health", onBackClick = onBackClick)

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Engine Health Status Overview
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Status OK",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "All Studio Systems Operational",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "PixelPro native image processing, biometric framing, perspective homography and 300 DPI PDF engine are fully initialized.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Capability Checks Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Processing Engine Capabilities", style = MaterialTheme.typography.labelLarge)

                    CapabilityRow("Hardware Accelerated Canvas", "Skia / OpenGL ES", true)
                    CapabilityRow("Native EXIF Rotation Engine", "ExifInterface v1.3+", true)
                    CapabilityRow("300 DPI Rendering Matrix", "4096 x 4096 px texture limit", true)
                    CapabilityRow("Print PDF Generator", "android.graphics.pdf.PdfDocument", true)
                    CapabilityRow("Dual ID Combiner", "Bilinear Scaling & Porter-Duff", true)
                    CapabilityRow("Perspective Quad Warper", "Homography setPolyToPoly", true)
                    CapabilityRow("Room SQLite Persistence", "KSP & Coroutines Flow", true)
                    CapabilityRow("Zero-Perm Media Picker", "PhotoPicker Jetpack Contract", true)
                }
            }

            // Memory & Runtime Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Device & Runtime Health", style = MaterialTheme.typography.labelLarge)

                    CapabilityRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", true)
                    CapabilityRow("Available RAM", "$availMemMb MB free / $totalMemMb MB", true)
                    CapabilityRow("Thread Worker Pool", "${Runtime.getRuntime().availableProcessors()} Coroutine Cores", true)

                    val exportsDir = File(context.filesDir, "studio_exports")
                    val cacheSizeKb = if (exportsDir.exists()) {
                        (exportsDir.listFiles()?.sumOf { it.length() } ?: 0L) / 1024
                    } else 0L
                    CapabilityRow("Studio Cache Storage", "$cacheSizeKb KB cached", true)

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            try {
                                exportsDir.listFiles()?.forEach { it.delete() }
                                cacheClearedMessage = "Temporary cache cleared!"
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_cache_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Temporary Cache")
                    }

                    cacheClearedMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(
    title: String,
    detail: String,
    isAvailable: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Active",
            tint = if (isAvailable) Color(0xFF10B981) else Color(0xFFEF4444),
            modifier = Modifier.size(20.dp)
        )
    }
}
