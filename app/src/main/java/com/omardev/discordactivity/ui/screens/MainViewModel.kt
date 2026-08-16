package com.omardev.discordactivity.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omardev.discordactivity.App
import com.omardev.discordactivity.data.models.ActivityPreset
import com.omardev.discordactivity.data.models.AppAnnouncement
import com.omardev.discordactivity.data.models.AppNotification
import com.omardev.discordactivity.data.models.DevicePlatform
import com.omardev.discordactivity.data.models.DiscordPresence
import com.omardev.discordactivity.data.models.DiscordUser
import com.omardev.discordactivity.data.models.NotificationLevel
import com.omardev.discordactivity.network.AdminNotifier
import com.omardev.discordactivity.network.DiscordApiClient
import com.omardev.discordactivity.network.GatewayConnectionState
import com.omardev.discordactivity.service.DiscordPresenceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as App).preferencesManager
    private val apiClient = DiscordApiClient()
    private val adminNotifier = AdminNotifier()
    private var saveJob: Job? = null

    val connectionState = DiscordPresenceService.connectionState
    val notificationsLog = DiscordPresenceService.notificationsLog

    private val _presets = MutableStateFlow(ActivityPreset.getDefaultPresets())
    val presets: StateFlow<List<ActivityPreset>> = _presets.asStateFlow()

    private val _selectedPresetName = MutableStateFlow(prefs.activePresetName)
    val selectedPresetName: StateFlow<String> = _selectedPresetName.asStateFlow()

    private val _selectedPlatform = MutableStateFlow(prefs.devicePlatform)
    val selectedPlatform: StateFlow<DevicePlatform> = _selectedPlatform.asStateFlow()

    private val _enableVrOverlay = MutableStateFlow(prefs.enableVrOverlay)
    val enableVrOverlay: StateFlow<Boolean> = _enableVrOverlay.asStateFlow()

    private val _token = MutableStateFlow(prefs.token)
    val token: StateFlow<String> = _token.asStateFlow()

    private val _verifiedUser = MutableStateFlow<DiscordUser?>(prefs.verifiedUser)
    val verifiedUser: StateFlow<DiscordUser?> = _verifiedUser.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _verificationMessage = MutableStateFlow<String?>(null)
    val verificationMessage: StateFlow<String?> = _verificationMessage.asStateFlow()

    private val _clientId = MutableStateFlow(prefs.clientId)
    val clientId: StateFlow<String> = _clientId.asStateFlow()

    private val _presence = MutableStateFlow(prefs.presence)
    val presence: StateFlow<DiscordPresence> = _presence.asStateFlow()

    // Admin & Announcement states
    private val _adminPin = MutableStateFlow(prefs.adminPin)
    val adminPin: StateFlow<String> = _adminPin.asStateFlow()

    private val _adminWebhookUrl = MutableStateFlow(prefs.adminWebhookUrl)
    val adminWebhookUrl: StateFlow<String> = _adminWebhookUrl.asStateFlow()

    private val _activeAnnouncement = MutableStateFlow<AppAnnouncement?>(prefs.activeAnnouncement)
    val activeAnnouncement: StateFlow<AppAnnouncement?> = _activeAnnouncement.asStateFlow()

    private val _showAnnouncementDialog = MutableStateFlow(false)
    val showAnnouncementDialog: StateFlow<Boolean> = _showAnnouncementDialog.asStateFlow()

    // Voice & AFK
    private val _enableVoiceStay = MutableStateFlow(prefs.enableVoiceStay)
    val enableVoiceStay: StateFlow<Boolean> = _enableVoiceStay.asStateFlow()

    private val _voiceChannelId = MutableStateFlow(prefs.voiceChannelId)
    val voiceChannelId: StateFlow<String> = _voiceChannelId.asStateFlow()

    private val _voiceMute = MutableStateFlow(prefs.voiceMute)
    val voiceMute: StateFlow<Boolean> = _voiceMute.asStateFlow()

    private val _voiceDeaf = MutableStateFlow(prefs.voiceDeaf)
    val voiceDeaf: StateFlow<Boolean> = _voiceDeaf.asStateFlow()

    private val _enableAfk = MutableStateFlow(prefs.enableAfk)
    val enableAfk: StateFlow<Boolean> = _enableAfk.asStateFlow()

    private val _afkMessage = MutableStateFlow(prefs.afkMessage)
    val afkMessage: StateFlow<String> = _afkMessage.asStateFlow()

    private val _afkReplyDms = MutableStateFlow(prefs.afkReplyDms)
    val afkReplyDms: StateFlow<Boolean> = _afkReplyDms.asStateFlow()

    private val _afkReplyMentions = MutableStateFlow(prefs.afkReplyMentions)
    val afkReplyMentions: StateFlow<Boolean> = _afkReplyMentions.asStateFlow()

    private val _afkCooldownSec = MutableStateFlow(prefs.afkCooldownSec)
    val afkCooldownSec: StateFlow<Int> = _afkCooldownSec.asStateFlow()

    init {
        checkPendingAnnouncement()
    }

    private fun checkPendingAnnouncement() {
        val announcement = prefs.activeAnnouncement
        val lastReadId = prefs.lastReadAnnouncementId
        if (announcement != null && announcement.id != lastReadId) {
            _showAnnouncementDialog.value = true
        }
    }

    fun dismissAnnouncement() {
        val announcement = _activeAnnouncement.value
        if (announcement != null) {
            prefs.lastReadAnnouncementId = announcement.id
        }
        _showAnnouncementDialog.value = false
    }

    fun publishAnnouncement(announcement: AppAnnouncement) {
        _activeAnnouncement.value = announcement
        _showAnnouncementDialog.value = true
        viewModelScope.launch(Dispatchers.IO) {
            prefs.activeAnnouncement = announcement
        }
        val current = DiscordPresenceService.notificationsLog.value.toMutableList()
        current.add(
            0,
            AppNotification(
                level = NotificationLevel.INFO,
                title = "Announcement Published",
                message = "${announcement.title} broadcasted successfully."
            )
        )
        DiscordPresenceService.notificationsLog.value = current
    }

    fun clearAnnouncement() {
        _activeAnnouncement.value = null
        _showAnnouncementDialog.value = false
        viewModelScope.launch(Dispatchers.IO) {
            prefs.activeAnnouncement = null
        }
    }

    fun setAdminWebhookUrl(url: String) {
        _adminWebhookUrl.value = url
        viewModelScope.launch(Dispatchers.IO) {
            prefs.adminWebhookUrl = url
        }
    }

    fun testAdminWebhook(url: String) {
        viewModelScope.launch {
            val result = adminNotifier.testWebhook(url)
            val current = DiscordPresenceService.notificationsLog.value.toMutableList()
            result.onSuccess {
                current.add(
                    0,
                    AppNotification(
                        level = NotificationLevel.SUCCESS,
                        title = "Webhook Test Success",
                        message = "Verification message sent to your Discord channel!"
                    )
                )
            }.onFailure { error ->
                current.add(
                    0,
                    AppNotification(
                        level = NotificationLevel.ERROR,
                        title = "Webhook Test Failed",
                        message = error.localizedMessage ?: "Failed to connect to Webhook."
                    )
                )
            }
            DiscordPresenceService.notificationsLog.value = current
        }
    }

    private fun triggerAdminJoinAlert() {
        val webhook = prefs.adminWebhookUrl
        if (webhook.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                adminNotifier.sendUserJoinAlert(
                    webhookUrl = webhook,
                    user = _verifiedUser.value,
                    platform = _selectedPlatform.value,
                    presence = _presence.value
                )
            }
        }
    }

    fun onTokenChanged(value: String) {
        _token.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.token = value
        }
    }

    fun verifyToken() {
        val currentToken = _token.value.trim()
        if (currentToken.isBlank()) {
            _verificationMessage.value = "❌ Please enter your Token first!"
            return
        }

        viewModelScope.launch {
            _isVerifying.value = true
            _verificationMessage.value = "🔄 Verifying Account Token..."

            val result = apiClient.verifyToken(currentToken)
            _isVerifying.value = false

            result.onSuccess { user ->
                _verifiedUser.value = user
                _verificationMessage.value = if (user.isUserToken) {
                    "✅ Account Verified: ${user.fullTag}"
                } else {
                    "✅ Bot Account Verified: ${user.displayName}"
                }

                viewModelScope.launch(Dispatchers.IO) {
                    prefs.verifiedUser = user
                    prefs.isUserToken = user.isUserToken
                }

                triggerAdminJoinAlert()

                val current = DiscordPresenceService.notificationsLog.value.toMutableList()
                current.add(
                    0,
                    AppNotification(
                        level = NotificationLevel.SUCCESS,
                        title = "Token Verified",
                        message = "Logged in as ${user.fullTag} (${if (user.isUserToken) "User Account" else "Bot Account"})"
                    )
                )
                DiscordPresenceService.notificationsLog.value = current
            }.onFailure { error ->
                _verificationMessage.value = "❌ ${error.localizedMessage ?: "Invalid Token!"}"
                val current = DiscordPresenceService.notificationsLog.value.toMutableList()
                current.add(
                    0,
                    AppNotification(
                        level = NotificationLevel.ERROR,
                        title = "Token Verification Failed",
                        message = error.localizedMessage ?: "Invalid Token"
                    )
                )
                DiscordPresenceService.notificationsLog.value = current
            }
        }
    }

    fun onClientIdChanged(value: String) {
        _clientId.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.clientId = value
        }
    }

    fun onPlatformSelected(platform: DevicePlatform) {
        _selectedPlatform.value = platform
        viewModelScope.launch(Dispatchers.IO) {
            prefs.devicePlatform = platform
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED) {
            pushPresenceUpdate()
        }
    }

    fun onEnableVrOverlayChanged(value: Boolean) {
        _enableVrOverlay.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.enableVrOverlay = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED) {
            pushPresenceUpdate()
        }
    }

    fun onPresetSelected(preset: ActivityPreset) {
        _selectedPresetName.value = preset.name
        val newPresence = preset.presence.copy(startTimestamp = System.currentTimeMillis())
        _presence.value = newPresence
        viewModelScope.launch(Dispatchers.IO) {
            prefs.activePresetName = preset.name
            prefs.presence = newPresence
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED) {
            pushPresenceUpdate()
        }
    }

    fun updatePresenceData(newPresence: DiscordPresence) {
        _presence.value = newPresence
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(400)
            prefs.presence = newPresence
        }
    }

    fun onEnableVoiceStayChanged(value: Boolean) {
        _enableVoiceStay.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.enableVoiceStay = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED) {
            pushPresenceUpdate()
        }
    }

    fun onVoiceChannelIdChanged(value: String) {
        _voiceChannelId.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.voiceChannelId = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableVoiceStay.value) {
            pushPresenceUpdate()
        }
    }

    fun onVoiceMuteChanged(value: Boolean) {
        _voiceMute.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.voiceMute = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableVoiceStay.value) {
            pushPresenceUpdate()
        }
    }

    fun onVoiceDeafChanged(value: Boolean) {
        _voiceDeaf.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.voiceDeaf = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableVoiceStay.value) {
            pushPresenceUpdate()
        }
    }

    fun onEnableAfkChanged(value: Boolean) {
        _enableAfk.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.enableAfk = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED) {
            pushPresenceUpdate()
        }
    }

    fun onAfkMessageChanged(value: String) {
        _afkMessage.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.afkMessage = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableAfk.value) {
            pushPresenceUpdate()
        }
    }

    fun onAfkReplyDmsChanged(value: Boolean) {
        _afkReplyDms.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.afkReplyDms = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableAfk.value) {
            pushPresenceUpdate()
        }
    }

    fun onAfkReplyMentionsChanged(value: Boolean) {
        _afkReplyMentions.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.afkReplyMentions = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableAfk.value) {
            pushPresenceUpdate()
        }
    }

    fun onAfkCooldownSecChanged(value: Int) {
        _afkCooldownSec.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.afkCooldownSec = value
        }
    }

    fun toggleService() {
        val app = getApplication<Application>()
        if (connectionState.value == GatewayConnectionState.DISCONNECTED ||
            connectionState.value == GatewayConnectionState.ERROR
        ) {
            DiscordPresenceService.startService(app)
            triggerAdminJoinAlert()
        } else {
            DiscordPresenceService.stopService(app)
        }
    }

    fun pushPresenceUpdate() {
        val app = getApplication<Application>()
        val intent = android.content.Intent(app, DiscordPresenceService::class.java).apply {
            action = DiscordPresenceService.ACTION_UPDATE_PRESENCE
        }
        app.startService(intent)
    }

    fun clearNotifications() {
        DiscordPresenceService.notificationsLog.value = emptyList()
    }
}
