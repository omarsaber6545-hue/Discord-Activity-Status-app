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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    val token by viewModel.token.collectAsState()
    val verifiedUser by viewModel.verifiedUser.collectAsState()
    val isVerifying by viewModel.isVerifying.collectAsState()
    val verificationMessage by viewModel.verificationMessage.collectAsState()
    val clientId by viewModel.clientId.collectAsState()
    val presence by viewModel.presence.collectAsState()

    // Admin & Announcement states
    val adminPin by viewModel.adminPin.collectAsState()
    val adminWebhookUrl by viewModel.adminWebhookUrl.collectAsState()
    val activeAnnouncement by viewModel.activeAnnouncement.collectAsState()
    val showAnnouncementDialog by viewModel.showAnnouncementDialog.collectAsState()

    // Voice & AFK
    val voiceChannelId by viewModel.voiceChannelId.collectAsState()
    val voiceMute by viewModel.voiceMute.collectAsState()
    val voiceDeaf by viewModel.voiceDeaf.collectAsState()
    val enableAfk by viewModel.enableAfk.collectAsState()
    val afkMessage by viewModel.afkMessage.collectAsState()
    val afkReplyDms by viewModel.afkReplyDms.collectAsState()
    val afkReplyMentions by viewModel.afkReplyMentions.collectAsState()

    var showNotificationSheet by remember { mutableStateOf(false) }
    var showAdminDashboard by remember { mutableStateOf(false) }
    var showTokenPassword by remember { mutableStateOf(false) }
    var isAdvancedExpanded by remember { mutableStateOf(true) }

    // Check if current verified user is Omar (rip_luufy25100 / ID: 1512205578015871048)
    val isAdminOwner = remember(verifiedUser) {
        val user = verifiedUser
        if (user != null) {
            val id = user.id.trim()
            val username = user.username.trim().lowercase()
            val fullTag = user.fullTag.trim().lowercase()
            id == "1512205578015871048" || username == "rip_luufy25100" || fullTag.contains("rip_luufy25100")
        } else false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (isAdminOwner) {
                                showAdminDashboard = true
                            }
                        }
                    ) {
                        Text(
                            text = "omar dev",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAdminOwner) AccentGold.copy(alpha = 0.25f) else DiscordBlurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isAdminOwner) "👑 ADMIN" else "v2.4 APK",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAdminOwner) AccentGold else DiscordBlurple,
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

            // 1. Live Discord Card Preview with real account avatar & responsive controls
            DiscordLiveCard(
                presence = presence,
                platform = selectedPlatform,
                connectionState = connectionState,
                verifiedUser = verifiedUser
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

            // 3. Platform & Spoofer Selector (PS5 / Xbox / VR / Mobile)
            DevicePlatformSelector(
                selectedPlatform = selectedPlatform,
                onPlatformSelected = { platform ->
                    viewModel.onPlatformSelected(platform)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Account Token Setup Card
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
                            text = "Account Token Setup",
                            style = MaterialTheme.typography.titleMedium,
                            color = DarkTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Account / Bot Token:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkTextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = token,
                        onValueChange = { viewModel.onTokenChanged(it) },
                        placeholder = { Text("Paste your Discord token here...") },
                        singleLine = true,
                        visualTransformation = if (showTokenPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(
                                onClick = { showTokenPassword = !showTokenPassword },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (showTokenPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = DarkTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showTokenPassword) "Hide" else "Show",
                                    color = DarkTextSecondary,
                                    fontSize = 12.sp
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Verify Account Token Button
                    Button(
                        onClick = { viewModel.verifyToken() },
                        enabled = !isVerifying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF35363C),
                            contentColor = DarkTextPrimary
                        )
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AccentCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying Account...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AccentCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🔒 Verify Account Token", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Verification Result Message
                    if (verificationMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = verificationMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (verificationMessage!!.startsWith("✅")) DiscordGreen else if (verificationMessage!!.startsWith("🔄")) DiscordYellow else DiscordRed,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Verified User Profile Box
                    if (verifiedUser != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = DarkInputBg,
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = verifiedUser!!.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = verifiedUser!!.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isAdminOwner) AccentGold.copy(alpha = 0.2f) else DiscordGreen.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (isAdminOwner) "DEVELOPER / ADMIN 👑" else "VERIFIED",
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isAdminOwner) AccentGold else DiscordGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${verifiedUser!!.fullTag} • ID: ${verifiedUser!!.id}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DarkTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Activity Customizer Section with Individual Toggle Switches
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Game Name
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Details (Line 1) with Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Details (Line 1)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = presence.enableDetails,
                            onCheckedChange = { viewModel.updatePresenceData(presence.copy(enableDetails = it)) },
                            colors = customSwitchColors()
                        )
                    }
                    if (presence.enableDetails) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = presence.details,
                            onValueChange = { viewModel.updatePresenceData(presence.copy(details = it)) },
                            label = { Text("Details Text") },
                            placeholder = { Text("Developing awesome apps 🚀") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = customTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // State (Line 2) with Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable State (Line 2)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = presence.enableState,
                            onCheckedChange = { viewModel.updatePresenceData(presence.copy(enableState = it)) },
                            colors = customSwitchColors()
                        )
                    }
                    if (presence.enableState) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = presence.state,
                            onValueChange = { viewModel.updatePresenceData(presence.copy(state = it)) },
                            label = { Text("State Text") },
                            placeholder = { Text("In Match (Score: 12 - 10)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = customTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Large Image with Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Large Image Asset",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = presence.enableLargeImage,
                            onCheckedChange = { viewModel.updatePresenceData(presence.copy(enableLargeImage = it)) },
                            colors = customSwitchColors()
                        )
                    }
                    if (presence.enableLargeImage) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = presence.largeImage,
                            onValueChange = { viewModel.updatePresenceData(presence.copy(largeImage = it)) },
                            label = { Text("Large Image URL (Discord CDN / PNG / JPG / WEBP)") },
                            placeholder = { Text("https://cdn.discordapp.com/attachments/...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = customTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = presence.largeText,
                            onValueChange = { viewModel.updatePresenceData(presence.copy(largeText = it)) },
                            label = { Text("Large Image Hover Text") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = customTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Small Image with Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Small Image Badge",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = presence.enableSmallImage,
                            onCheckedChange = { viewModel.updatePresenceData(presence.copy(enableSmallImage = it)) },
                            colors = customSwitchColors()
                        )
                    }
                    if (presence.enableSmallImage) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = presence.smallImage,
                            onValueChange = { viewModel.updatePresenceData(presence.copy(smallImage = it)) },
                            label = { Text("Small Image URL (Discord CDN / PNG / JPG)") },
                            placeholder = { Text("https://cdn.discordapp.com/attachments/...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = customTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = presence.smallText,
                            onValueChange = { viewModel.updatePresenceData(presence.copy(smallText = it)) },
                            label = { Text("Small Image Hover Text") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = customTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = presence.showTimer,
                            onCheckedChange = { viewModel.updatePresenceData(presence.copy(showTimer = it)) },
                            colors = customSwitchColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = DarkCardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Button 1 with Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Button 1 🔗",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = presence.enableButton1,
                            onCheckedChange = { viewModel.updatePresenceData(presence.copy(enableButton1 = it)) },
                            colors = customSwitchColors()
                        )
                    }
                    if (presence.enableButton1) {
                        Spacer(modifier = Modifier.height(4.dp))
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
                        Spacer(modifier = Modifier.height(4.dp))
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button 2 with Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Button 2 🔗",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = presence.enableButton2,
                            onCheckedChange = { viewModel.updatePresenceData(presence.copy(enableButton2 = it)) },
                            colors = customSwitchColors()
                        )
                    }
                    if (presence.enableButton2) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = presence.button2Label,
                            onValueChange = { viewModel.updatePresenceData(presence.copy(button2Label = it)) },
                            label = { Text("Button 2 Label") },
                            placeholder = { Text("GitHub: Omar-Dev") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = customTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = presence.button2Url,
                            onValueChange = { viewModel.updatePresenceData(presence.copy(button2Url = it)) },
                            label = { Text("Button 2 URL") },
                            placeholder = { Text("https://github.com/omarsaber6545-hue") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = customTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
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
                            // Voice Channel Stay
                            Text(
                                text = "🎙️ 24/7 Voice Stay Settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

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

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = DarkCardBorder)
                            Spacer(modifier = Modifier.height(14.dp))

                            // AFK Responder Section with ON/OFF Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🤖 Enable AFK Auto-Responder",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (enableAfk) DiscordGreen else DarkTextPrimary
                                    )
                                }
                                Switch(
                                    checked = enableAfk,
                                    onCheckedChange = { viewModel.onEnableAfkChanged(it) },
                                    colors = customSwitchColors()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

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
                                    Text("Reply to DMs (الخاص)", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = afkReplyMentions,
                                        onCheckedChange = { viewModel.onAfkReplyMentionsChanged(it) }
                                    )
                                    Text("Reply to Mentions (المنشن)", style = MaterialTheme.typography.bodyMedium)
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

        // In-App Announcement Popup Dialog (Automatic upon open / new announcement)
        if (showAnnouncementDialog && activeAnnouncement != null) {
            AnnouncementDialog(
                announcement = activeAnnouncement!!,
                onDismiss = { viewModel.dismissAnnouncement() }
            )
        }

        // Admin Dashboard Dialog
        if (showAdminDashboard) {
            AdminDashboardDialog(
                currentPin = adminPin,
                currentWebhookUrl = adminWebhookUrl,
                activeAnnouncement = activeAnnouncement,
                onSaveWebhookUrl = { viewModel.setAdminWebhookUrl(it) },
                onTestWebhook = { viewModel.testAdminWebhook(it) },
                onPublishAnnouncement = { viewModel.publishAnnouncement(it) },
                onClearAnnouncement = { viewModel.clearAnnouncement() },
                onDismiss = { showAdminDashboard = false }
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

@Composable
private fun customSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = DarkTextPrimary,
    checkedTrackColor = DiscordBlurple,
    uncheckedThumbColor = DarkTextMuted,
    uncheckedTrackColor = DarkInputBg
)
