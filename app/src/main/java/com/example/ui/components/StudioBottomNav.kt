package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.navigation.StudioDestination

@Composable
fun StudioBottomNav(
    currentDestination: StudioDestination,
    onDestinationSelected: (StudioDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    // Show top 5 flagship destinations in the bottom bar for optimal spacing
    val bottomDestinations = listOf(
        StudioDestination.DASHBOARD,
        StudioDestination.PASSPORT,
        StudioDestination.ID_CARD,
        StudioDestination.BACKGROUND,
        StudioDestination.DOCUMENT,
        StudioDestination.PROJECTS
    )

    NavigationBar(
        modifier = modifier,
        windowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        bottomDestinations.forEach { destination ->
            val isSelected = currentDestination == destination

            val (filledIcon, outlinedIcon) = when (destination) {
                StudioDestination.DASHBOARD -> Pair(Icons.Filled.Home, Icons.Outlined.Home)
                StudioDestination.PASSPORT -> Pair(Icons.Filled.Face, Icons.Outlined.Face)
                StudioDestination.ID_CARD -> Pair(Icons.Filled.Badge, Icons.Outlined.Badge)
                StudioDestination.BACKGROUND -> Pair(Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh)
                StudioDestination.DOCUMENT -> Pair(Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner)
                StudioDestination.PROJECTS -> Pair(Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary)
                StudioDestination.HEALTH -> Pair(Icons.Filled.Home, Icons.Outlined.Home)
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                        contentDescription = destination.title
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
            )
        }
    }
}
