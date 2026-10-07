package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class StudioColorPreset(
    val name: String,
    val color: Color,
    val isTransparent: Boolean = false
)

@Composable
fun ColorPickerRow(
    selectedColor: Color,
    onColorSelected: (Color, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        StudioColorPreset("White", Color.White),
        StudioColorPreset("Off-White", Color(0xFFF8FAFC)),
        StudioColorPreset("Sky Blue", Color(0xFFE0F2FE)),
        StudioColorPreset("Studio Blue", Color(0xFF0284C7)),
        StudioColorPreset("Light Grey", Color(0xFFE2E8F0)),
        StudioColorPreset("Neutral Dark", Color(0xFF1E293B)),
        StudioColorPreset("Velvet Red", Color(0xFFDC2626)),
        StudioColorPreset("Warm Sand", Color(0xFFFEF3C7)),
        StudioColorPreset("Transparent", Color.Transparent, isTransparent = true)
    )

    Column(modifier = modifier) {
        Text(
            text = "Backdrop Color",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { preset ->
                val isSelected = if (preset.isTransparent) {
                    selectedColor == Color.Transparent
                } else {
                    selectedColor == preset.color
                }

                Box(
                    modifier = Modifier
                        .testTag("color_preset_${preset.name.lowercase().replace(" ", "_")}")
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (preset.isTransparent) Color(0xFF334155) else preset.color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8).copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(preset.color, preset.isTransparent) },
                    contentAlignment = Alignment.Center
                ) {
                    if (preset.isTransparent) {
                        Text(
                            text = "PNG",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    } else if (isSelected) {
                        val iconTint = if (preset.color == Color.White || preset.color == Color(0xFFF8FAFC) || preset.color == Color(0xFFE0F2FE) || preset.color == Color(0xFFE2E8F0) || preset.color == Color(0xFFFEF3C7)) {
                            Color.Black
                        } else {
                            Color.White
                        }
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected ${preset.name}",
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
