package com.pingplace.ui.common

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
    accentColor: Color = MeadowGreen
) {
    FilterChip(
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
