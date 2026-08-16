package com.omardev.discordactivity.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omardev.discordactivity.data.models.DevicePlatform
import com.omardev.discordactivity.data.models.DiscordPresence
import com.omardev.discordactivity.network.GatewayConnectionState
import com.omardev.discordactivity.ui.components.*
import com.omardev.discordactivity.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    val notifications by viewModel.notificationsLog.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val selectedPresetName by viewModel.selectedPresetName.collectAsState()
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()
    val botToken by viewModel.botToken.collectAsState()
    val clientId by viewModel.clientId.collectAsState()
    val presence by viewModel.presence.collectAsState()

    // Voice & AFK
    val voiceChannelId by viewModel.voiceChannelId.collectAsState()
    val voiceMute by viewModel.voiceMute.collectAsState()
    val voiceDeaf by viewModel.voiceDeaf.collectAsState()
    val afkMessage by viewModel.afkMessage.collectAsState()
    val afkReplyDms by viewModel.afkReplyDms.collectAsState()
    val afkReplyMentions by viewModel.afkReplyMentions.collectAsState()

    var showNotificationSheet by remember { mutableStateOf(false) }
    var showTokenPassword by remember { mutableStateOf(false) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "omar dev",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DiscordBlurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "v2.3 APK",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = DiscordBlurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // Notification Bell Button with counter badge
                    IconButton(onClick = { showNotificationSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (notifications.isNotEmpty()) {
                                    Badge(
                                        containerColor = DiscordRed,
                                        contentColor = DarkTextPrimary
                                    ) {
                                        Text(text = "${notifications.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (notifications.isNotEmpty()) DiscordYellow else DarkTextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg
                )
            )
        },
        containerColor = DarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Live Discord Card Preview
            DiscordLiveCard(
                presence = presence,
                platform = selectedPlatform,
                connectionState = connectionState
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Quick Presets Selector
            PresetSelector(
                presets = presets,
                selectedPresetName = selectedPresetName,
                onPresetSelected = { preset ->
                    viewModel.onPresetSelected(preset)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Platform & Spoofer Selector
            DevicePlatformSelector(
                selectedPlatform = selectedPlatform,
                onPlatformSelected = { platform ->
                    viewModel.onPlatformSelected(platform)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Discord Bot Credentials Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DISCORD BOT CREDENTIALS",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { viewModel.onTokenChanged(it) },
                        label = { Text("Discord Bot Token") },
                        placeholder = { Text("MTAx...") },
                        singleLine = true,
                        visualTransformation = if (showTokenPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showTokenPassword = !showTokenPassword }) {
                                Icon(
                                    imageVector = if (showTokenPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = DarkTextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkInputBg,
                            unfocusedContainerColor = DarkInputBg,
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { viewModel.onClientIdChanged(it) },
                        label = { Text("Application / Client ID") },
                        placeholder = { Text("1536494151074586624") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkInputBg,
                            unfocusedContainerColor = DarkInputBg,
                            focusedBorderColor = DiscordBlurple,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Activity Customizer Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = DiscordBlurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CUSTOMIZE ACTIVITY DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = presence.gameName,
                        onValueChange = { viewModel.updatePresenceData(presence.copy(gameName = it)) },
                        label = { Text("Game / Activity Name") },
                        placeholder = { Text("Playing omar dev") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = presence.details,
                        onValueChange = { viewModel.updatePresenceData(presence.copy(details = it)) },
                        label = { Text("Details (Line 1)") },
                        placeholder = { Text("Developing awesome apps 🚀") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = presence.state,
                        onValueChange = { viewModel.updatePresenceData(presence.copy(state = it)) },
                        label = { Text("State (Line 2)") },
                        placeholder = { Text("In Match (Score: 12 - 10)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = presence.largeImage,
                            onValueChange = { viewModel.updatePresenceData(presence.copy(largeImage = it)) },
                            label = { Text("Large Image URL") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = customTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = presence.smallImage,
                            onValueChange = { viewModel.updatePresenceData(presence.copy(smallImage = it)) },
                            label = { Text("Small Image URL") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = customTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Timer Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show Elapsed Timer ⏱️",
                            style = MaterialTheme.typography.bodyLarge,
                            color = DarkTextPrimary
                        )
                        Switch(
                            checked = presence.showTimer,
                            onCheckedChange = { viewModel.updatePresenceData(presence.copy(showTimer = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkTextPrimary,
                                checkedTrackColor = DiscordBlurple
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons Labels & URLs
                    OutlinedTextField(
                        value = presence.button1Label,
                        onValueChange = { viewModel.updatePresenceData(presence.copy(button1Label = it)) },
                        label = { Text("Button 1 Label") },
                        placeholder = { Text("Omar Dev Site") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = presence.button1Url,
                        onValueChange = { viewModel.updatePresenceData(presence.copy(button1Url = it)) },
                        label = { Text("Button 1 URL") },
                        placeholder = { Text("https://omar-dev.site") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Advanced 24/7 Voice Channel & AFK Responder (Collapsible)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAdvancedExpanded = !isAdvancedExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "24/7 VOICE STAY & AFK RESPONDER",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = DarkTextSecondary
                        )
                    }

                    AnimatedVisibility(visible = isAdvancedExpanded) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            OutlinedTextField(
                                value = voiceChannelId,
                                onValueChange = { viewModel.onVoiceChannelIdChanged(it) },
                                label = { Text("Voice Channel ID") },
                                placeholder = { Text("1491185683459735709") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = voiceMute,
                                        onCheckedChange = { viewModel.onVoiceMuteChanged(it) }
                                    )
                                    Text("Self Mute", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = voiceDeaf,
                                        onCheckedChange = { viewModel.onVoiceDeafChanged(it) }
                                    )
                                    Text("Self Deaf", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = DarkCardBorder)
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = afkMessage,
                                onValueChange = { viewModel.onAfkMessageChanged(it) },
                                label = { Text("AFK Auto-Reply Message") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                colors = customTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = afkReplyDms,
                                        onCheckedChange = { viewModel.onAfkReplyDmsChanged(it) }
                                    )
                                    Text("Reply to DMs", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = afkReplyMentions,
                                        onCheckedChange = { viewModel.onAfkReplyMentionsChanged(it) }
                                    )
                                    Text("Reply to Mentions", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Master Action Buttons
            val isConnected = connectionState == GatewayConnectionState.IDENTIFIED ||
                    connectionState == GatewayConnectionState.CONNECTING ||
                    connectionState == GatewayConnectionState.CONNECTED

            Button(
                onClick = { viewModel.toggleService() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) DiscordRed else DiscordBlurple
                )
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "STOP DISCORD PRESENCE" else "START DISCORD PRESENCE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isConnected) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { viewModel.pushPresenceUpdate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(AccentCyan, DiscordBlurple))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = AccentCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UPDATE ACTIVE PRESENCE",
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }

        // Notification Center Sheet
        if (showNotificationSheet) {
            NotificationCenterSheet(
                notifications = notifications,
                onClearAll = { viewModel.clearNotifications() },
                onDismiss = { showNotificationSheet = false }
            )
        }
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DarkInputBg,
    unfocusedContainerColor = DarkInputBg,
    focusedBorderColor = DiscordBlurple,
    unfocusedBorderColor = DarkCardBorder,
    focusedTextColor = DarkTextPrimary,
    unfocusedTextColor = DarkTextPrimary,
    focusedLabelColor = DiscordBlurple,
    unfocusedLabelColor = DarkTextSecondary
)
