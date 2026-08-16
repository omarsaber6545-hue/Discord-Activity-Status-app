package com.omardev.discordactivity.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omardev.discordactivity.App
import com.omardev.discordactivity.data.models.ActivityPreset
import com.omardev.discordactivity.data.models.AppNotification
import com.omardev.discordactivity.data.models.DevicePlatform
import com.omardev.discordactivity.data.models.DiscordPresence
import com.omardev.discordactivity.data.models.DiscordUser
import com.omardev.discordactivity.data.models.NotificationLevel
import com.omardev.discordactivity.network.DiscordApiClient
import com.omardev.discordactivity.service.DiscordPresenceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as App).preferencesManager
    private val apiClient = DiscordApiClient()

    val connectionState = DiscordPresenceService.connectionState
    val notificationsLog = DiscordPresenceService.notificationsLog

    private val _presets = MutableStateFlow(ActivityPreset.getDefaultPresets())
    val presets: StateFlow<List<ActivityPreset>> = _presets.asStateFlow()

    private val _selectedPresetName = MutableStateFlow(prefs.activePresetName)
    val selectedPresetName: StateFlow<String> = _selectedPresetName.asStateFlow()

    private val _selectedPlatform = MutableStateFlow(prefs.devicePlatform)
    val selectedPlatform: StateFlow<DevicePlatform> = _selectedPlatform.asStateFlow()

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

    // Voice & AFK
    private val _voiceChannelId = MutableStateFlow(prefs.voiceChannelId)
    val voiceChannelId: StateFlow<String> = _voiceChannelId.asStateFlow()

    private val _voiceMute = MutableStateFlow(prefs.voiceMute)
    val voiceMute: StateFlow<Boolean> = _voiceMute.asStateFlow()

    private val _voiceDeaf = MutableStateFlow(prefs.voiceDeaf)
    val voiceDeaf: StateFlow<Boolean> = _voiceDeaf.asStateFlow()

    private val _afkMessage = MutableStateFlow(prefs.afkMessage)
    val afkMessage: StateFlow<String> = _afkMessage.asStateFlow()

    private val _afkReplyDms = MutableStateFlow(prefs.afkReplyDms)
    val afkReplyDms: StateFlow<Boolean> = _afkReplyDms.asStateFlow()

    private val _afkReplyMentions = MutableStateFlow(prefs.afkReplyMentions)
    val afkReplyMentions: StateFlow<Boolean> = _afkReplyMentions.asStateFlow()

    private val _afkCooldownSec = MutableStateFlow(prefs.afkCooldownSec)
    val afkCooldownSec: StateFlow<Int> = _afkCooldownSec.asStateFlow()

    fun onTokenChanged(value: String) {
        _token.value = value
        prefs.token = value
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
                prefs.verifiedUser = user
                prefs.isUserToken = user.isUserToken
                _verificationMessage.value = if (user.isUserToken) {
                    "✅ Account Verified: ${user.fullTag}"
                } else {
                    "✅ Bot Account Verified: ${user.displayName}"
                }

                // Add to Notification Center
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
        prefs.clientId = value
    }

    fun onPlatformSelected(platform: DevicePlatform) {
        _selectedPlatform.value = platform
        prefs.devicePlatform = platform
    }

    fun onPresetSelected(preset: ActivityPreset) {
        _selectedPresetName.value = preset.name
        prefs.activePresetName = preset.name
        _presence.value = preset.presence.copy(startTimestamp = System.currentTimeMillis())
        prefs.presence = _presence.value
    }

    fun updatePresenceData(newPresence: DiscordPresence) {
        _presence.value = newPresence
        prefs.presence = newPresence
    }

    fun onVoiceChannelIdChanged(value: String) {
        _voiceChannelId.value = value
        prefs.voiceChannelId = value
    }

    fun onVoiceMuteChanged(value: Boolean) {
        _voiceMute.value = value
        prefs.voiceMute = value
    }

    fun onVoiceDeafChanged(value: Boolean) {
        _voiceDeaf.value = value
        prefs.voiceDeaf = value
    }

    fun onAfkMessageChanged(value: String) {
        _afkMessage.value = value
        prefs.afkMessage = value
    }

    fun onAfkReplyDmsChanged(value: Boolean) {
        _afkReplyDms.value = value
        prefs.afkReplyDms = value
    }

    fun onAfkReplyMentionsChanged(value: Boolean) {
        _afkReplyMentions.value = value
        prefs.afkReplyMentions = value
    }

    fun onAfkCooldownSecChanged(value: Int) {
        _afkCooldownSec.value = value
        prefs.afkCooldownSec = value
    }

    fun toggleService() {
        val app = getApplication<Application>()
        if (connectionState.value == com.omardev.discordactivity.network.GatewayConnectionState.DISCONNECTED ||
            connectionState.value == com.omardev.discordactivity.network.GatewayConnectionState.ERROR
        ) {
            DiscordPresenceService.startService(app)
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
