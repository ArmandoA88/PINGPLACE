package com.pingplace.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pingplace.ui.theme.Ember
import com.pingplace.ui.theme.MeadowGreen

@Composable
fun SelectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val scale by animateFloatAsState(if (selected) 1.03f else 1f, spring(dampingRatio = 0.65f), label = "chip selection")
    FilterChip(
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun BlockedSelectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    SelectionChip(
        label = label,
        selected = selected,
        onClick = onClick,
        accentColor = Ember
    )
}
