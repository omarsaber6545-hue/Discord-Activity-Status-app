package com.omardev.discordactivity.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.omardev.discordactivity.data.models.ActivityPreset
import com.omardev.discordactivity.data.models.DevicePlatform
import com.omardev.discordactivity.data.models.DiscordPresence

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("discord_activity_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_DEVICE_PLATFORM = "device_platform"
        private const val KEY_VOICE_CHANNEL_ID = "voice_channel_id"
        private const val KEY_VOICE_MUTE = "voice_mute"
        private const val KEY_VOICE_DEAF = "voice_deaf"
        private const val KEY_AFK_MESSAGE = "afk_message"
        private const val KEY_AFK_REPLY_DMS = "afk_reply_dms"
        private const val KEY_AFK_REPLY_MENTIONS = "afk_reply_mentions"
        private const val KEY_AFK_COOLDOWN = "afk_cooldown"
        private const val KEY_ACTIVE_PRESET = "active_preset"
        private const val KEY_CURRENT_PRESENCE = "current_presence"
    }

    var botToken: String
        get() = prefs.getString(KEY_BOT_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BOT_TOKEN, value).apply()

    var clientId: String
        get() = prefs.getString(KEY_CLIENT_ID, "1536494151074586624") ?: "1536494151074586624"
        set(value) = prefs.edit().putString(KEY_CLIENT_ID, value).apply()

    var devicePlatform: DevicePlatform
        get() {
            val name = prefs.getString(KEY_DEVICE_PLATFORM, DevicePlatform.VR.name) ?: DevicePlatform.VR.name
            return try {
                DevicePlatform.valueOf(name)
            } catch (e: Exception) {
                DevicePlatform.VR
            }
        }
        set(value) = prefs.edit().putString(KEY_DEVICE_PLATFORM, value.name).apply()

    var voiceChannelId: String
        get() = prefs.getString(KEY_VOICE_CHANNEL_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_VOICE_CHANNEL_ID, value).apply()

    var voiceMute: Boolean
        get() = prefs.getBoolean(KEY_VOICE_MUTE, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_MUTE, value).apply()

    var voiceDeaf: Boolean
        get() = prefs.getBoolean(KEY_VOICE_DEAF, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_DEAF, value).apply()

    var afkMessage: String
        get() = prefs.getString(KEY_AFK_MESSAGE, "انا قافل شويه عشان تعبان لما هفتح هرد عليك (رد تلقائي)") ?: ""
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
}
