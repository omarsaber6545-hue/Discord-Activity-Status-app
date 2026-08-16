package com.omardev.discordactivity.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omardev.discordactivity.data.models.AppNotification
import com.omardev.discordactivity.data.models.NotificationLevel
import com.omardev.discordactivity.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    notifications: List<AppNotification>,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<NotificationLevel?>(null) }

    val filteredList = remember(notifications, selectedFilter) {
        if (selectedFilter == null) notifications
        else notifications.filter { it.level == selectedFilter }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        contentColor = DarkTextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = DiscordYellow
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notification Center (${notifications.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )
                }

                if (notifications.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text(text = "Clear All", color = DiscordRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = selectedFilter == NotificationLevel.ERROR,
                    onClick = { selectedFilter = NotificationLevel.ERROR },
                    label = { Text("❌ Errors", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = selectedFilter == NotificationLevel.SUCCESS,
                    onClick = { selectedFilter = NotificationLevel.SUCCESS },
                    label = { Text("🟢 Success", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = selectedFilter == NotificationLevel.INFO,
                    onClick = { selectedFilter = NotificationLevel.INFO },
                    label = { Text("ℹ️ Info", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No events logged yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DarkTextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        val icon = when (item.level) {
                            NotificationLevel.SUCCESS -> Icons.Default.CheckCircle to DiscordGreen
                            NotificationLevel.ERROR -> Icons.Default.Error to DiscordRed
                            NotificationLevel.WARNING -> Icons.Default.Warning to DiscordYellow
                            NotificationLevel.INFO -> Icons.Default.Info to AccentCyan
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = DarkInputBg,
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(icon.second.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon.first,
                                        contentDescription = null,
                                        tint = icon.second,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkTextPrimary
                                        )
                                        Text(
                                            text = item.formattedTime,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = DarkTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = item.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DarkTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
