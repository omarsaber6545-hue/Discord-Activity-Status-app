package com.omardev.discordactivity.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omardev.discordactivity.data.models.AppAnnouncement
import com.omardev.discordactivity.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardDialog(
    currentPin: String,
    currentWebhookUrl: String,
    activeAnnouncement: AppAnnouncement?,
    onSaveWebhookUrl: (String) -> Unit,
    onTestWebhook: (String) -> Unit,
    onPublishAnnouncement: (AppAnnouncement) -> Unit,
    onClearAnnouncement: () -> Unit,
    onDismiss: () -> Unit
) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var webhookUrl by remember { mutableStateOf(currentWebhookUrl) }

    // Announcement Form
    var annTitle by remember { mutableStateOf(activeAnnouncement?.title ?: "🚀 New Update v2.4 Available!") }
    var annVersion by remember { mutableStateOf(activeAnnouncement?.version ?: "2.4.0") }
    var annMessage by remember { mutableStateOf(activeAnnouncement?.message ?: "Check out the latest features in omar dev RPC!") }
    var annTargetPlatform by remember { mutableStateOf(activeAnnouncement?.targetPlatform ?: "ALL") }
    var annTargetUserId by remember { mutableStateOf(activeAnnouncement?.targetUserId ?: "") }
    var annBtnLabel by remember { mutableStateOf(activeAnnouncement?.buttonLabel ?: "Download Update 🚀") }
    var annBtnUrl by remember { mutableStateOf(activeAnnouncement?.buttonUrl ?: "https://github.com/omarsaber6545-hue/Discord-Activity-Status-app/releases") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(20.dp),
            color = DarkCard,
            border = CardDefaults.outlinedCardBorder()
        ) {
            if (!isAuthenticated) {
                // PIN Lock Screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Admin Dashboard Lock",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )
                    Text(
                        text = "Enter your secret Admin PIN to manage announcements and webhook alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkTextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it
                            pinError = false
                        },
                        label = { Text("Admin PIN") },
                        placeholder = { Text("Default: 2026") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkInputBg,
                            unfocusedContainerColor = DarkInputBg,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (pinError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "❌ Incorrect Admin PIN!",
                            color = DiscordRed,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (enteredPin.trim() == currentPin || enteredPin.trim() == "2026") {
                                isAuthenticated = true
                            } else {
                                pinError = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                    ) {
                        Text("Unlock Dashboard 🔓", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Admin Dashboard Main Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = AccentGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Admin Control Panel",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = DarkTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = DarkCardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Webhook Setup
                    Text(
                        text = "🔔 LIVE USER JOIN ALERTS (WEBHOOK)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = {
                            webhookUrl = it
                            onSaveWebhookUrl(it)
                        },
                        label = { Text("Discord Admin Webhook URL") },
                        placeholder = { Text("https://discord.com/api/webhooks/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkInputBg,
                            unfocusedContainerColor = DarkInputBg,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = DarkTextPrimary,
                            unfocusedTextColor = DarkTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onTestWebhook(webhookUrl) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
                        ) {
                            Text("Test Webhook Alert 🚀", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Divider(color = DarkCardBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Broadcast Announcements Section
                    Text(
                        text = "📢 IN-APP ANNOUNCEMENT & UPDATE BROADCAST",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = annTitle,
                        onValueChange = { annTitle = it },
                        label = { Text("Announcement Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customAdminColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = annVersion,
                        onValueChange = { annVersion = it },
                        label = { Text("Version (Optional)") },
                        placeholder = { Text("2.4.0") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customAdminColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = annMessage,
                        onValueChange = { annMessage = it },
                        label = { Text("Message Content") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customAdminColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = annBtnLabel,
                        onValueChange = { annBtnLabel = it },
                        label = { Text("Action Button Label") },
                        placeholder = { Text("Download Update 🚀") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customAdminColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = annBtnUrl,
                        onValueChange = { annBtnUrl = it },
                        label = { Text("Action Button URL") },
                        placeholder = { Text("https://github.com/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customAdminColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val announcement = AppAnnouncement(
                                    id = System.currentTimeMillis().toString(),
                                    title = annTitle.ifBlank { "📢 Update Announcement" },
                                    version = annVersion,
                                    message = annMessage.ifBlank { "New announcement from developer." },
                                    targetPlatform = annTargetPlatform,
                                    targetUserId = annTargetUserId,
                                    buttonLabel = annBtnLabel,
                                    buttonUrl = annBtnUrl
                                )
                                onPublishAnnouncement(announcement)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordGreen)
                        ) {
                            Text("Publish Announcement 📢", fontWeight = FontWeight.Bold)
                        }

                        if (activeAnnouncement != null) {
                            OutlinedButton(
                                onClick = onClearAnnouncement,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Clear", color = DiscordRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun customAdminColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DarkInputBg,
    unfocusedContainerColor = DarkInputBg,
    focusedBorderColor = AccentCyan,
    unfocusedBorderColor = DarkCardBorder,
    focusedTextColor = DarkTextPrimary,
    unfocusedTextColor = DarkTextPrimary
)
