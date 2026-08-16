package com.omardev.discordactivity.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.omardev.discordactivity.data.models.DevicePlatform
import com.omardev.discordactivity.data.models.DiscordPresence
import com.omardev.discordactivity.data.models.DiscordUser
import com.omardev.discordactivity.network.GatewayConnectionState
import com.omardev.discordactivity.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DiscordLiveCard(
    presence: DiscordPresence,
    platform: DevicePlatform,
    connectionState: GatewayConnectionState,
    verifiedUser: DiscordUser? = null,
    modifier: Modifier = Modifier
) {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(presence.startTimestamp, presence.showTimer) {
        while (true) {
            if (presence.showTimer) {
                val now = System.currentTimeMillis()
                elapsedSeconds = ((now - presence.startTimestamp) / 1000).coerceAtLeast(0)
            }
            delay(1000)
        }
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = if (hours > 0) {
        String.format("%02d:%02d:%02d elapsed", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d elapsed", minutes, seconds)
    }

    val statusColor = when (connectionState) {
        GatewayConnectionState.IDENTIFIED -> DiscordGreen
        GatewayConnectionState.CONNECTING,
        GatewayConnectionState.CONNECTED -> DiscordYellow
        GatewayConnectionState.ERROR -> DiscordRed
        GatewayConnectionState.DISCONNECTED -> DarkTextMuted
    }

    val displayName = verifiedUser?.displayName ?: "omar dev"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(DiscordBlurple.copy(alpha = 0.6f), Color.Transparent, AccentCyan.copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Bar: Profile preview & Platform badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(42.dp)) {
                        if (verifiedUser?.avatar != null) {
                            AsyncImage(
                                model = verifiedUser.avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(DiscordBlurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = DiscordBlurple,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Live Status Dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(statusColor)
                                .border(2.dp, DarkCard, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = DarkTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            if (verifiedUser != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = DiscordBlurple.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (verifiedUser.isUserToken) "USER" else "BOT",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DiscordBlurple,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = when (connectionState) {
                                GatewayConnectionState.IDENTIFIED -> "Active: ${platform.title}"
                                GatewayConnectionState.CONNECTING -> "Connecting..."
                                GatewayConnectionState.CONNECTED -> "Handshake..."
                                GatewayConnectionState.ERROR -> "Offline / Error"
                                GatewayConnectionState.DISCONNECTED -> "Offline"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor
                        )
                    }
                }

                // Platform Badge Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkInputBg,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = platform.icon, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = platform.id.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = DarkCardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Activity Details Section
            Text(
                text = "PLAYING A GAME",
                style = MaterialTheme.typography.labelSmall,
                color = DarkTextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Large & Small Image Asset
                if (presence.enableLargeImage && presence.largeImage.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkInputBg)
                    ) {
                        AsyncImage(
                            model = presence.largeImage,
                            contentDescription = presence.largeText,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Small image badge
                        if (presence.enableSmallImage && presence.smallImage.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .clip(CircleShape)
                                    .background(DarkCard)
                                    .border(2.dp, DarkCard, CircleShape)
                            ) {
                                AsyncImage(
                                    model = presence.smallImage,
                                    contentDescription = presence.smallText,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }

                // Activity Texts
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = presence.gameName.ifBlank { platform.title },
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (presence.enableDetails && presence.details.isNotBlank()) {
                        Text(
                            text = presence.details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (presence.enableState && presence.state.isNotBlank()) {
                        Text(
                            text = presence.state,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (presence.showTimer) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Interactive Buttons Preview
            val hasBtn1 = presence.enableButton1 && presence.button1Label.isNotBlank()
            val hasBtn2 = presence.enableButton2 && presence.button2Label.isNotBlank()
            if (hasBtn1 || hasBtn2) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasBtn1) {
                        PreviewButton(label = presence.button1Label)
                    }
                    if (hasBtn2) {
                        PreviewButton(label = presence.button2Label)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewButton(label: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = DarkInputBg,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = DarkTextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = DarkTextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
