package com.omardev.discordactivity.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.omardev.discordactivity.data.models.ActivityPreset
import com.omardev.discordactivity.data.models.AppAnnouncement
import com.omardev.discordactivity.data.models.DevicePlatform
import com.omardev.discordactivity.data.models.DiscordPresence
import com.omardev.discordactivity.data.models.DiscordUser

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("discord_activity_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_TOKEN = "account_token"
        private const val KEY_IS_USER_TOKEN = "is_user_token"
        private const val KEY_VERIFIED_USER = "verified_user"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_DEVICE_PLATFORM = "device_platform"

        // Dual Simultaneous Mode
        private const val KEY_ENABLE_DUAL_MODE = "enable_dual_mode"
        private const val KEY_SECONDARY_PLATFORM = "secondary_platform"
        private const val KEY_SECONDARY_GAME_NAME = "secondary_game_name"
        private const val KEY_SECONDARY_ENABLE_DETAILS = "secondary_enable_details"
        private const val KEY_SECONDARY_DETAILS = "secondary_details"
        private const val KEY_SECONDARY_ENABLE_STATE = "secondary_enable_state"
        private const val KEY_SECONDARY_STATE = "secondary_state"
        private const val KEY_SECONDARY_ENABLE_AFK = "secondary_enable_afk"
        private const val KEY_SECONDARY_SHOW_TIMER = "secondary_show_timer"

        // Voice Stay
        private const val KEY_ENABLE_VOICE_STAY = "enable_voice_stay"
        private const val KEY_VOICE_CHANNEL_ID = "voice_channel_id"
        private const val KEY_VOICE_MUTE = "voice_mute"
        private const val KEY_VOICE_DEAF = "voice_deaf"

        // AFK
        private const val KEY_ENABLE_AFK = "enable_afk"
        private const val KEY_AFK_MESSAGE = "afk_message"
        private const val KEY_AFK_REPLY_DMS = "afk_reply_dms"
        private const val KEY_AFK_REPLY_MENTIONS = "afk_reply_mentions"
        private const val KEY_AFK_COOLDOWN = "afk_cooldown"

        private const val KEY_ACTIVE_PRESET = "active_preset"
        private const val KEY_CURRENT_PRESENCE = "current_presence"

        // Admin & Announcement keys
        private const val KEY_ADMIN_PIN = "admin_pin"
        private const val KEY_ADMIN_WEBHOOK = "admin_webhook_url"
        private const val KEY_LAST_READ_ANNOUNCEMENT = "last_read_announcement"
        private const val KEY_ACTIVE_ANNOUNCEMENT = "active_announcement"
    }

    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var isUserToken: Boolean
        get() = prefs.getBoolean(KEY_IS_USER_TOKEN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_USER_TOKEN, value).apply()

    var verifiedUser: DiscordUser?
        get() {
            val json = prefs.getString(KEY_VERIFIED_USER, null) ?: return null
            return try {
                gson.fromJson(json, DiscordUser::class.java)
            } catch (e: Exception) {
                null
            }
        }
        set(value) {
            val json = if (value != null) gson.toJson(value) else null
            prefs.edit().putString(KEY_VERIFIED_USER, json).apply()
        }

    var clientId: String
        get() = prefs.getString(KEY_CLIENT_ID, "1536494151074586624") ?: "1536494151074586624"
        set(value) = prefs.edit().putString(KEY_CLIENT_ID, value).apply()

    var devicePlatform: DevicePlatform
        get() {
            val name = prefs.getString(KEY_DEVICE_PLATFORM, DevicePlatform.PS5.name) ?: DevicePlatform.PS5.name
            return try {
                DevicePlatform.valueOf(name)
            } catch (e: Exception) {
                DevicePlatform.PS5
            }
        }
        set(value) {
            prefs.edit().putString(KEY_DEVICE_PLATFORM, value.name).apply()
        }

    // Dual Simultaneous Mode
    var enableDualMode: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_DUAL_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_DUAL_MODE, value).apply()

    var secondaryPlatform: DevicePlatform
        get() {
            val name = prefs.getString(KEY_SECONDARY_PLATFORM, DevicePlatform.VR.name) ?: DevicePlatform.VR.name
            return try {
                DevicePlatform.valueOf(name)
            } catch (e: Exception) {
                DevicePlatform.VR
            }
        }
        set(value) {
            prefs.edit().putString(KEY_SECONDARY_PLATFORM, value.name).apply()
        }

    var secondaryGameName: String
        get() = prefs.getString(KEY_SECONDARY_GAME_NAME, "Virtual Reality VR 🥽") ?: "Virtual Reality VR 🥽"
        set(value) = prefs.edit().putString(KEY_SECONDARY_GAME_NAME, value).apply()

    var secondaryEnableDetails: Boolean
        get() = prefs.getBoolean(KEY_SECONDARY_ENABLE_DETAILS, true)
        set(value) = prefs.edit().putBoolean(KEY_SECONDARY_ENABLE_DETAILS, value).apply()

    var secondaryDetails: String
        get() = prefs.getString(KEY_SECONDARY_DETAILS, "Playing in VR") ?: "Playing in VR"
        set(value) = prefs.edit().putString(KEY_SECONDARY_DETAILS, value).apply()

    var secondaryEnableState: Boolean
        get() = prefs.getBoolean(KEY_SECONDARY_ENABLE_STATE, true)
        set(value) = prefs.edit().putBoolean(KEY_SECONDARY_ENABLE_STATE, value).apply()

    var secondaryState: String
        get() = prefs.getString(KEY_SECONDARY_STATE, "Meta Quest 3 Active") ?: "Meta Quest 3 Active"
        set(value) = prefs.edit().putString(KEY_SECONDARY_STATE, value).apply()

    var secondaryEnableAfk: Boolean
        get() = prefs.getBoolean(KEY_SECONDARY_ENABLE_AFK, false)
        set(value) = prefs.edit().putBoolean(KEY_SECONDARY_ENABLE_AFK, value).apply()

    var secondaryShowTimer: Boolean
        get() = prefs.getBoolean(KEY_SECONDARY_SHOW_TIMER, true)
        set(value) = prefs.edit().putBoolean(KEY_SECONDARY_SHOW_TIMER, value).apply()

    // 24/7 Voice Stay
    var enableVoiceStay: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_VOICE_STAY, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_VOICE_STAY, value).apply()

    var voiceChannelId: String
        get() = prefs.getString(KEY_VOICE_CHANNEL_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_VOICE_CHANNEL_ID, value).apply()

    var voiceMute: Boolean
        get() = prefs.getBoolean(KEY_VOICE_MUTE, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_MUTE, value).apply()

    var voiceDeaf: Boolean
        get() = prefs.getBoolean(KEY_VOICE_DEAF, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_DEAF, value).apply()

    // AFK System
    var enableAfk: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_AFK, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_AFK, value).apply()

    var afkMessage: String
        get() = prefs.getString(KEY_AFK_MESSAGE, "انا غير متواجد حالياً، سأقوم بالرد عليك فور عودتي! ☕ (رد تلقائي)") ?: ""
        set(value) = prefs.edit().putString(KEY_AFK_MESSAGE, value).apply()

    var afkReplyDms: Boolean
        get() = prefs.getBoolean(KEY_AFK_REPLY_DMS, true)
        set(value) = prefs.edit().putBoolean(KEY_AFK_REPLY_DMS, value).apply()

    var afkReplyMentions: Boolean
        get() = prefs.getBoolean(KEY_AFK_REPLY_MENTIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_AFK_REPLY_MENTIONS, value).apply()

    var afkCooldownSec: Int
        get() = prefs.getInt(KEY_AFK_COOLDOWN, 15)
        set(value) = prefs.edit().putInt(KEY_AFK_COOLDOWN, value).apply()

    var activePresetName: String
        get() = prefs.getString(KEY_ACTIVE_PRESET, "VS Code") ?: "VS Code"
        set(value) = prefs.edit().putString(KEY_ACTIVE_PRESET, value).apply()

    var presence: DiscordPresence
        get() {
            val json = prefs.getString(KEY_CURRENT_PRESENCE, null)
            return if (json != null) {
                try {
                    gson.fromJson(json, DiscordPresence::class.java) ?: ActivityPreset.getDefaultPresets().first().presence
                } catch (e: Exception) {
                    ActivityPreset.getDefaultPresets().first().presence
                }
            } else {
                ActivityPreset.getDefaultPresets().first().presence
            }
        }
        set(value) {
            val json = gson.toJson(value)
            prefs.edit().putString(KEY_CURRENT_PRESENCE, json).apply()
        }

    // Admin & Announcements
    var adminPin: String
        get() = prefs.getString(KEY_ADMIN_PIN, "9510953600") ?: "9510953600"
        set(value) = prefs.edit().putString(KEY_ADMIN_PIN, value).apply()

    var adminWebhookUrl: String
        get() = prefs.getString(KEY_ADMIN_WEBHOOK, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ADMIN_WEBHOOK, value).apply()

    var lastReadAnnouncementId: String
        get() = prefs.getString(KEY_LAST_READ_ANNOUNCEMENT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_READ_ANNOUNCEMENT, value).apply()

    var activeAnnouncement: AppAnnouncement?
        get() {
            val json = prefs.getString(KEY_ACTIVE_ANNOUNCEMENT, null) ?: return null
            return try {
                gson.fromJson(json, AppAnnouncement::class.java)
            } catch (e: Exception) {
                null
            }
        }
        set(value) {
            val json = if (value != null) gson.toJson(value) else null
            prefs.edit().putString(KEY_ACTIVE_ANNOUNCEMENT, json).apply()
        }
}
