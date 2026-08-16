package com.omardev.discordactivity.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omardev.discordactivity.data.models.DevicePlatform
import com.omardev.discordactivity.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePlatformSelector(
    selectedPlatform: DevicePlatform,
    onPlatformSelected: (DevicePlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    val platforms = DevicePlatform.values()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            platforms.forEach { platform ->
                val isSelected = platform == selectedPlatform
                FilterChip(
                    selected = isSelected,
                    onClick = { onPlatformSelected(platform) },
                    label = {
                        Text(
                            text = "${platform.icon} ${platform.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkInputBg,
                        labelColor = DarkTextSecondary,
                        selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                        selectedLabelColor = AccentCyan
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) AccentCyan else DarkCardBorder,
                        selectedBorderColor = AccentCyan,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}
