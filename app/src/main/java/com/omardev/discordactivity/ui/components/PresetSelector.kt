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
import com.omardev.discordactivity.data.models.ActivityPreset
import com.omardev.discordactivity.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSelector(
    presets: List<ActivityPreset>,
    selectedPresetName: String,
    onPresetSelected: (ActivityPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "⚡ QUICK GAME PRESETS",
            style = MaterialTheme.typography.labelSmall,
            color = DarkTextSecondary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = preset.name == selectedPresetName
                FilterChip(
                    selected = isSelected,
                    onClick = { onPresetSelected(preset) },
                    label = {
                        Text(
                            text = "${preset.icon} ${preset.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkInputBg,
                        labelColor = DarkTextSecondary,
                        selectedContainerColor = DiscordBlurple,
                        selectedLabelColor = DarkTextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) DiscordBlurple else DarkCardBorder,
                        selectedBorderColor = DiscordBlurple,
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
