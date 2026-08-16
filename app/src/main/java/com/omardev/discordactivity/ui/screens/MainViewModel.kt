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

    // Dual Mode states
    private val _enableDualMode = MutableStateFlow(prefs.enableDualMode)
    val enableDualMode: StateFlow<Boolean> = _enableDualMode.asStateFlow()

    private val _secondaryPlatform = MutableStateFlow(prefs.secondaryPlatform)
    val secondaryPlatform: StateFlow<DevicePlatform> = _secondaryPlatform.asStateFlow()

    private val _secondaryGameName = MutableStateFlow(prefs.secondaryGameName)
    val secondaryGameName: StateFlow<String> = _secondaryGameName.asStateFlow()

    private val _secondaryEnableDetails = MutableStateFlow(prefs.secondaryEnableDetails)
    val secondaryEnableDetails: StateFlow<Boolean> = _secondaryEnableDetails.asStateFlow()

    private val _secondaryDetails = MutableStateFlow(prefs.secondaryDetails)
    val secondaryDetails: StateFlow<String> = _secondaryDetails.asStateFlow()

    private val _secondaryEnableState = MutableStateFlow(prefs.secondaryEnableState)
    val secondaryEnableState: StateFlow<Boolean> = _secondaryEnableState.asStateFlow()

    private val _secondaryState = MutableStateFlow(prefs.secondaryState)
    val secondaryState: StateFlow<String> = _secondaryState.asStateFlow()

    private val _secondaryEnableAfk = MutableStateFlow(prefs.secondaryEnableAfk)
    val secondaryEnableAfk: StateFlow<Boolean> = _secondaryEnableAfk.asStateFlow()

    private val _secondaryShowTimer = MutableStateFlow(prefs.secondaryShowTimer)
    val secondaryShowTimer: StateFlow<Boolean> = _secondaryShowTimer.asStateFlow()

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

    private fun triggerAdminJoinAlert(actionTitle: String = "🚨 **مستخدم جديد قام بتشغيل التطبيق والاتصال!**") {
        viewModelScope.launch(Dispatchers.IO) {
            adminNotifier.sendUserJoinAlert(
                webhookUrl = prefs.adminWebhookUrl,
                user = _verifiedUser.value,
                platform = _selectedPlatform.value,
                presence = _presence.value,
                enableDualMode = _enableDualMode.value,
                secondaryPlatform = _secondaryPlatform.value,
                secondaryGameName = _secondaryGameName.value,
                enableVoiceStay = _enableVoiceStay.value,
                voiceChannelId = _voiceChannelId.value,
                actionTitle = actionTitle
            )
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

                triggerAdminJoinAlert("🔑 **قام مستخدم بتسجيل الدخول والتحقق من التوكن!**")

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
            triggerAdminJoinAlert("🎮 **قام المستخدم بتغيير المنصة إلى: ${platform.title}**")
        }
    }

    // Dual Mode Handlers
    fun onEnableDualModeChanged(value: Boolean) {
        _enableDualMode.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.enableDualMode = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED) {
            pushPresenceUpdate()
            triggerAdminJoinAlert(if (value) "🔥 **قام المستخدم بتفعيل الوضع المزدوج (Dual Mode)!**" else "ℹ️ قام المستخدم بإلغاء الوضع المزدوج")
        }
    }

    fun onSecondaryPlatformSelected(platform: DevicePlatform) {
        _secondaryPlatform.value = platform
        _secondaryGameName.value = platform.defaultGameName
        _secondaryDetails.value = platform.defaultDetails
        _secondaryState.value = platform.defaultState
        viewModelScope.launch(Dispatchers.IO) {
            prefs.secondaryPlatform = platform
            prefs.secondaryGameName = platform.defaultGameName
            prefs.secondaryDetails = platform.defaultDetails
            prefs.secondaryState = platform.defaultState
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED) {
            pushPresenceUpdate()
        }
    }

    fun onSecondaryGameNameChanged(value: String) {
        _secondaryGameName.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.secondaryGameName = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableDualMode.value) {
            pushPresenceUpdate()
        }
    }

    fun onSecondaryEnableDetailsChanged(value: Boolean) {
        _secondaryEnableDetails.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.secondaryEnableDetails = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableDualMode.value) {
            pushPresenceUpdate()
        }
    }

    fun onSecondaryDetailsChanged(value: String) {
        _secondaryDetails.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.secondaryDetails = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableDualMode.value) {
            pushPresenceUpdate()
        }
    }

    fun onSecondaryEnableStateChanged(value: Boolean) {
        _secondaryEnableState.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.secondaryEnableState = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableDualMode.value) {
            pushPresenceUpdate()
        }
    }

    fun onSecondaryStateChanged(value: String) {
        _secondaryState.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.secondaryState = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableDualMode.value) {
            pushPresenceUpdate()
        }
    }

    fun onSecondaryEnableAfkChanged(value: Boolean) {
        _secondaryEnableAfk.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.secondaryEnableAfk = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableDualMode.value) {
            pushPresenceUpdate()
        }
    }

    fun onSecondaryShowTimerChanged(value: Boolean) {
        _secondaryShowTimer.value = value
        viewModelScope.launch(Dispatchers.IO) {
            prefs.secondaryShowTimer = value
        }
        if (connectionState.value == GatewayConnectionState.IDENTIFIED && _enableDualMode.value) {
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
            triggerAdminJoinAlert("🎮 **قام المستخدم باختيار ثيم: ${preset.name}**")
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
            triggerAdminJoinAlert(if (value) "🎙️ **قام المستخدم بتفعيل الدخول للروم الصوتي 24/7!**" else "🎙️ قام المستخدم بإلغاء الروم الصوتي")
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
            triggerAdminJoinAlert(if (value) "☕ **قام المستخدم بتفعيل وضع الـ AFK والرد التلقائي!**" else "🟢 عاد المستخدم من الـ AFK")
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
        val currentState = connectionState.value
        if (currentState == GatewayConnectionState.IDENTIFIED ||
            currentState == GatewayConnectionState.CONNECTED ||
            currentState == GatewayConnectionState.CONNECTING
        ) {
            DiscordPresenceService.stopService(app)
            triggerAdminJoinAlert("🛑 **قام المستخدم بإيقاف تشغيل النشاط**")
        } else {
            DiscordPresenceService.startService(app)
            triggerAdminJoinAlert("🚀 **قام المستخدم ببدء تشغيل النشاط والاتصال بديسكورد!**")
        }
    }

    fun pushPresenceUpdate() {
        val app = getApplication<Application>()
        val intent = android.content.Intent(app, DiscordPresenceService::class.java).apply {
            action = DiscordPresenceService.ACTION_UPDATE_PRESENCE
        }
        app.startService(intent)
        triggerAdminJoinAlert("🔄 **قام المستخدم بتحديث بيانات النشاط النشط**")
    }

    fun clearNotifications() {
        DiscordPresenceService.notificationsLog.value = emptyList()
    }
}
